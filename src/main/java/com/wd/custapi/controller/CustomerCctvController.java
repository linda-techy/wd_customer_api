package com.wd.custapi.controller;

import com.wd.custapi.dto.CctvStreamDto;
import com.wd.custapi.dto.CustomerCctvCameraDto;
import com.wd.custapi.exception.ResourceNotFoundException;
import com.wd.custapi.model.CctvCamera;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CctvCameraRepository;
import com.wd.custapi.service.CctvStreamProxyService;
import com.wd.custapi.service.DashboardService;
import com.wd.custapi.util.StreamUrlSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/projects/{projectId}/cctv-cameras")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'CUSTOMER_ADMIN')")
public class CustomerCctvController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerCctvController.class);
    private static final String HLS_CONTENT_TYPE = "application/vnd.apple.mpegurl";

    private final CctvCameraRepository cameraRepository;
    private final DashboardService dashboardService;
    private final CctvStreamProxyService streamProxy;

    public CustomerCctvController(CctvCameraRepository cameraRepository,
                                   DashboardService dashboardService,
                                   CctvStreamProxyService streamProxy) {
        this.cameraRepository = cameraRepository;
        this.dashboardService = dashboardService;
        this.streamProxy = streamProxy;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCameras(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<CustomerCctvCameraDto> cameras = cameraRepository
                    .findByProjectIdAndIsActiveTrueOrderByDisplayOrder(project.getId())
                    .stream()
                    .map(CustomerCctvCameraDto::from)
                    .toList();
            return ResponseEntity.ok(Map.of("cameras", cameras, "count", cameras.size()));
        } catch (Exception e) {
            logger.error("Failed to fetch CCTV cameras for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch cameras"));
        }
    }

    /**
     * Stream-URL broker endpoint.
     *
     * <p>Returns a sanitized stream URL for a single active camera that belongs to the
     * caller's project. Credentials are never included in the response:
     * <ul>
     *   <li>The {@link CctvStreamDto} record has no username/password fields.</li>
     *   <li>Any "user:pass@" userinfo embedded in the stored {@code stream_url} is
     *       stripped by {@link StreamUrlSanitizer#stripCredentials} before the value
     *       is placed in the DTO.</li>
     * </ul>
     *
     * <p>Returns HTTP 404 for:
     * <ul>
     *   <li>Cameras that do not exist.</li>
     *   <li>Cameras that belong to a different project.</li>
     *   <li>Inactive cameras ({@code is_active = false}).</li>
     *   <li>Projects the caller is not a member of (the membership guard in
     *       {@link DashboardService#getProjectByUuidAndEmail} throws before the
     *       camera lookup is attempted).</li>
     * </ul>
     */
    @GetMapping("/{cameraId}/stream")
    public ResponseEntity<CctvStreamDto> getCameraStream(
            @PathVariable("projectId") String projectUuid,
            @PathVariable("cameraId") Long cameraId,
            Authentication auth) {
        String email = auth.getName();
        // Membership guard — throws RuntimeException("Project not found or access denied")
        // for non-members, which GlobalExceptionHandler maps to HTTP 500. Inactive /
        // unknown cameras are caught below and mapped to 404 via ResourceNotFoundException.
        Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

        CctvCamera camera = cameraRepository
                .findByIdAndProjectIdAndIsActiveTrue(cameraId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CctvCamera", cameraId));

        String safeUrl = StreamUrlSanitizer.stripCredentials(camera.getStreamUrl());

        CctvStreamDto dto = new CctvStreamDto(
                camera.getId(),
                safeUrl,
                camera.getStreamProtocol());

        return ResponseEntity.ok(dto);
    }

    /**
     * HLS manifest proxy. Fetches the camera's manifest server-side (injecting
     * credentials) and rewrites every segment / variant / key URI to route back
     * through {@link #getCameraStreamSegment}. The customer's player points here
     * and never sees the camera host or its credentials. Non-HTTP cameras (RTSP)
     * return 501 — they need a transcoder, which is out of scope for this proxy.
     */
    @GetMapping(value = "/{cameraId}/stream.m3u8", produces = HLS_CONTENT_TYPE)
    public ResponseEntity<byte[]> getCameraHlsManifest(
            @PathVariable("projectId") String projectUuid,
            @PathVariable("cameraId") Long cameraId,
            Authentication auth) {
        CctvCamera camera = requireCamera(projectUuid, cameraId, auth);
        String cleanBase = StreamUrlSanitizer.stripCredentials(camera.getStreamUrl());
        URI baseUri;
        try {
            baseUri = URI.create(cleanBase);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        String scheme = baseUri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(("Live playback for " + scheme + " streams is not supported "
                            + "(requires a server-side transcoder).").getBytes(StandardCharsets.UTF_8));
        }
        try {
            CctvStreamProxyService.UpstreamResponse up = streamProxy.fetch(baseUri, authHeaderFor(camera));
            if (up.status() >= 400) {
                logger.warn("Upstream camera {} manifest fetch returned {}", cameraId, up.status());
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            String rewritten = streamProxy.rewriteHlsManifest(
                    new String(up.body(), StandardCharsets.UTF_8), baseUri);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, HLS_CONTENT_TYPE)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(rewritten.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("CCTV manifest proxy failed for camera {}", cameraId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    /**
     * Segment proxy. The {@code s} token encodes the absolute upstream URL; it is
     * resolved and SSRF-checked (must be the camera's own host over http(s))
     * before being fetched with credentials. A nested media playlist is rewritten
     * too so its segments stay proxied; binary segments are relayed as-is.
     */
    @GetMapping("/{cameraId}/segment")
    public ResponseEntity<byte[]> getCameraStreamSegment(
            @PathVariable("projectId") String projectUuid,
            @PathVariable("cameraId") Long cameraId,
            @RequestParam("s") String token,
            Authentication auth) {
        CctvCamera camera = requireCamera(projectUuid, cameraId, auth);
        String cleanBase = StreamUrlSanitizer.stripCredentials(camera.getStreamUrl());
        URI upstream;
        try {
            upstream = streamProxy.resolveUpstreamSegment(cleanBase, token);
        } catch (SecurityException e) {
            logger.warn("Blocked CCTV segment proxy (SSRF guard) for camera {}: {}", cameraId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            CctvStreamProxyService.UpstreamResponse up = streamProxy.fetch(upstream, authHeaderFor(camera));
            if (up.status() >= 400) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            if (streamProxy.isHlsManifest(up.contentType(), up.body())) {
                String rewritten = streamProxy.rewriteHlsManifest(
                        new String(up.body(), StandardCharsets.UTF_8), upstream);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, HLS_CONTENT_TYPE)
                        .header(HttpHeaders.CACHE_CONTROL, "no-store")
                        .body(rewritten.getBytes(StandardCharsets.UTF_8));
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, up.contentType())
                    .body(up.body());
        } catch (Exception e) {
            logger.error("CCTV segment proxy failed for camera {}", cameraId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    /** Ownership-checked active-camera lookup (404 if missing / other project / inactive). */
    private CctvCamera requireCamera(String projectUuid, Long cameraId, Authentication auth) {
        Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, auth.getName());
        return cameraRepository.findByIdAndProjectIdAndIsActiveTrue(cameraId, project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CctvCamera", cameraId));
    }

    /** Basic-auth from the camera's columns, falling back to userinfo in the stream URL. */
    private String authHeaderFor(CctvCamera camera) {
        String header = streamProxy.basicAuthHeader(camera.getUsername(), camera.getPassword());
        if (header != null) {
            return header;
        }
        try {
            String userInfo = URI.create(camera.getStreamUrl()).getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                return streamProxy.basicAuthHeader(parts[0], parts[1]);
            }
        } catch (RuntimeException ignored) {
            // fall through — no usable credentials
        }
        return null;
    }
}
