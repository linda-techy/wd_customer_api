package com.wd.custapi.controller;

import com.wd.custapi.exception.ResourceNotFoundException;
import com.wd.custapi.model.CctvCamera;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CctvCameraRepository;
import com.wd.custapi.service.CctvStreamProxyService;
import com.wd.custapi.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Guard tests for the CCTV HLS reverse-proxy endpoints on CustomerCctvController.
 * The proxy's pure logic (rewrite / SSRF / auth) is covered by
 * {@code CctvStreamProxyServiceTest}; here we verify the controller's orchestration:
 *
 * <ul>
 *   <li>RTSP / non-HTTP cameras → 501 (need a transcoder, out of scope).</li>
 *   <li>Segment whose host ≠ camera host → 403 (SSRF guard surfaced as Forbidden).</li>
 *   <li>Unknown / inactive / other-project camera → ResourceNotFoundException (404).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CustomerCctvProxyControllerTest {

    @Mock private CctvCameraRepository cameraRepository;
    @Mock private DashboardService dashboardService;
    @Mock private CctvStreamProxyService streamProxy;
    @Mock private Authentication auth;

    @InjectMocks
    private CustomerCctvController controller;

    private static final String PROJECT_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final Long PROJECT_ID = 5L;
    private static final Long CAMERA_ID = 42L;

    @BeforeEach
    void setUp() {
        Project project = new Project();
        project.setId(PROJECT_ID);
        when(auth.getName()).thenReturn("member@example.com");
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString())).thenReturn(project);
    }

    @Test
    void rtspCamera_manifestEndpoint_returns501() {
        CctvCamera cam = cameraStub("rtsp://192.168.1.10:554/live");
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(CAMERA_ID, PROJECT_ID))
                .thenReturn(Optional.of(cam));

        ResponseEntity<byte[]> resp = controller.getCameraHlsManifest(PROJECT_UUID, CAMERA_ID, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(501);
        assertThat(new String(resp.getBody())).contains("transcoder");
    }

    @Test
    void segmentEndpoint_ssrfGuardViolation_returns403() {
        CctvCamera cam = cameraStub("https://cam.example.com/live/stream.m3u8");
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(CAMERA_ID, PROJECT_ID))
                .thenReturn(Optional.of(cam));
        // Proxy rejects a token that resolves to a different host (SSRF attempt).
        when(streamProxy.resolveUpstreamSegment(anyString(), anyString()))
                .thenThrow(new SecurityException("host mismatch"));

        ResponseEntity<byte[]> resp =
                controller.getCameraStreamSegment(PROJECT_UUID, CAMERA_ID, "dGFtcGVyZWQ", auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void unknownCamera_manifestEndpoint_throwsResourceNotFound() {
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(CAMERA_ID, PROJECT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getCameraHlsManifest(PROJECT_UUID, CAMERA_ID, auth))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private CctvCamera cameraStub(String streamUrl) {
        CctvCamera cam = mock(CctvCamera.class);
        when(cam.getStreamUrl()).thenReturn(streamUrl);
        return cam;
    }
}
