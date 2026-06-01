package com.wd.custapi.controller;

import com.sun.net.httpserver.HttpServer;
import com.wd.custapi.model.CctvCamera;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CctvCameraRepository;
import com.wd.custapi.service.CctvStreamProxyService;
import com.wd.custapi.service.DashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * End-to-end test for the CCTV HLS reverse-proxy on a REAL local HTTP server
 * (no external network, no DB). Wires a real {@link CctvStreamProxyService}
 * into {@link CustomerCctvController} with mocked repo/dashboard/auth, then
 * points the camera at a local {@link HttpServer} that serves real HLS content
 * — proving end-to-end:
 *
 * <ol>
 *   <li>The manifest is fetched server-side and segment URIs are rewritten to
 *       absolute, opaque proxy refs (no upstream host or path leaks).</li>
 *   <li>Segment bytes are relayed verbatim with the upstream content-type.</li>
 *   <li>Stored {@code username}/{@code password} are injected server-side as
 *       a Basic-auth header — credential-protected cameras play through.</li>
 * </ol>
 */
class CctvStreamProxyIntegrationTest {

    private static final String UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final Long PROJECT_ID = 5L;
    private static final Long CAMERA_ID = 42L;

    // A minimal real HLS media playlist with a single relative segment ref.
    private static final String MANIFEST =
            "#EXTM3U\n"
          + "#EXT-X-VERSION:3\n"
          + "#EXT-X-TARGETDURATION:4\n"
          + "#EXT-X-MEDIA-SEQUENCE:0\n"
          + "#EXTINF:4.0,\n"
          + "seg0.ts\n"
          + "#EXT-X-ENDLIST\n";
    private static final byte[] SEGMENT_BYTES = "TSDATA-binary-payload".getBytes(StandardCharsets.UTF_8);

    private HttpServer server;
    private String base;                // http://127.0.0.1:<random>
    private CustomerCctvController controller;
    private CctvCameraRepository cameraRepository;
    private Authentication auth;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        // Public manifest + segment (no auth required).
        server.createContext("/live/stream.m3u8", exch -> {
            byte[] body = MANIFEST.getBytes(StandardCharsets.UTF_8);
            exch.getResponseHeaders().add("Content-Type", "application/vnd.apple.mpegurl");
            exch.sendResponseHeaders(200, body.length);
            try (var os = exch.getResponseBody()) { os.write(body); }
        });
        server.createContext("/live/seg0.ts", exch -> {
            exch.getResponseHeaders().add("Content-Type", "video/mp2t");
            exch.sendResponseHeaders(200, SEGMENT_BYTES.length);
            try (var os = exch.getResponseBody()) { os.write(SEGMENT_BYTES); }
        });
        // Credential-protected variant — proves server-side auth injection works.
        server.createContext("/protected/stream.m3u8", exch -> {
            String authz = exch.getRequestHeaders().getFirst("Authorization");
            String expected = "Basic " + Base64.getEncoder()
                    .encodeToString("admin:s3cr3t".getBytes(StandardCharsets.UTF_8));
            if (!expected.equals(authz)) {
                exch.sendResponseHeaders(401, -1);
                exch.close();
                return;
            }
            byte[] body = MANIFEST.getBytes(StandardCharsets.UTF_8);
            exch.getResponseHeaders().add("Content-Type", "application/vnd.apple.mpegurl");
            exch.sendResponseHeaders(200, body.length);
            try (var os = exch.getResponseBody()) { os.write(body); }
        });

        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();

        // Wire the controller with a REAL proxy service + mocked surroundings.
        cameraRepository = mock(CctvCameraRepository.class);
        DashboardService dashboardService = mock(DashboardService.class);
        auth = mock(Authentication.class);
        Project project = new Project();
        project.setId(PROJECT_ID);
        when(auth.getName()).thenReturn("member@example.com");
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString())).thenReturn(project);
        controller = new CustomerCctvController(cameraRepository, dashboardService, new CctvStreamProxyService());
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void manifestProxy_fetchesUpstreamAndRewritesSegmentRefs() {
        CctvCamera cam = cameraWith(base + "/live/stream.m3u8", null, null);
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(anyLong(), anyLong()))
                .thenReturn(Optional.of(cam));

        ResponseEntity<byte[]> resp = controller.getCameraHlsManifest(UUID, CAMERA_ID, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getHeaders().getFirst("Content-Type")).contains("mpegurl");

        String body = new String(resp.getBody(), StandardCharsets.UTF_8);
        // The raw upstream segment ref is replaced; the rewritten ref encodes the ABSOLUTE upstream URL.
        String expectedToken = CctvStreamProxyService.encodeRef(base + "/live/seg0.ts");
        assertThat(body)
                .contains("segment?s=" + expectedToken)
                // Directives preserved
                .contains("#EXTM3U").contains("#EXT-X-ENDLIST")
                // Upstream host / path never appears in the rewritten manifest
                .doesNotContain("127.0.0.1")
                .doesNotContain("/live/seg0.ts");
    }

    @Test
    void segmentProxy_relaysUpstreamBytesVerbatim() {
        CctvCamera cam = cameraWith(base + "/live/stream.m3u8", null, null);
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(anyLong(), anyLong()))
                .thenReturn(Optional.of(cam));
        String token = CctvStreamProxyService.encodeRef(base + "/live/seg0.ts");

        ResponseEntity<byte[]> resp = controller.getCameraStreamSegment(UUID, CAMERA_ID, token, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getHeaders().getFirst("Content-Type")).isEqualTo("video/mp2t");
        assertThat(resp.getBody()).isEqualTo(SEGMENT_BYTES);
    }

    @Test
    void manifestProxy_injectsBasicAuthFromStoredCredentials() {
        // Upstream rejects requests without Basic admin:s3cr3t.
        CctvCamera cam = cameraWith(base + "/protected/stream.m3u8", "admin", "s3cr3t");
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(anyLong(), anyLong()))
                .thenReturn(Optional.of(cam));

        ResponseEntity<byte[]> resp = controller.getCameraHlsManifest(UUID, CAMERA_ID, auth);

        // 200 = the proxy injected the right Authorization header server-side.
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        String body = new String(resp.getBody(), StandardCharsets.UTF_8);
        assertThat(body).contains("#EXTM3U");
    }

    @Test
    void manifestProxy_missingCredentialsForProtectedUpstream_returns502() {
        // Same protected endpoint, no creds on the camera → upstream 401 → proxy 502.
        CctvCamera cam = cameraWith(base + "/protected/stream.m3u8", null, null);
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(anyLong(), anyLong()))
                .thenReturn(Optional.of(cam));

        ResponseEntity<byte[]> resp = controller.getCameraHlsManifest(UUID, CAMERA_ID, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(502);
    }

    private CctvCamera cameraWith(String streamUrl, String username, String password) {
        CctvCamera cam = mock(CctvCamera.class);
        when(cam.getStreamUrl()).thenReturn(streamUrl);
        when(cam.getUsername()).thenReturn(username);
        when(cam.getPassword()).thenReturn(password);
        return cam;
    }
}
