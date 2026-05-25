package com.wd.custapi.controller;

import com.wd.custapi.dto.CctvStreamDto;
import com.wd.custapi.dto.CustomerCctvCameraDto;
import com.wd.custapi.exception.ResourceNotFoundException;
import com.wd.custapi.model.CctvCamera;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CctvCameraRepository;
import com.wd.custapi.service.DashboardService;
import com.wd.custapi.util.StreamUrlSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/projects/{projectId}/cctv-cameras")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'CUSTOMER_ADMIN')")
public class CustomerCctvController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerCctvController.class);

    private final CctvCameraRepository cameraRepository;
    private final DashboardService dashboardService;

    public CustomerCctvController(CctvCameraRepository cameraRepository,
                                   DashboardService dashboardService) {
        this.cameraRepository = cameraRepository;
        this.dashboardService = dashboardService;
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
}
