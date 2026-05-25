package com.wd.custapi.controller;

import com.wd.custapi.dto.CctvStreamDto;
import com.wd.custapi.exception.ResourceNotFoundException;
import com.wd.custapi.model.CctvCamera;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.enums.StreamProtocol;
import com.wd.custapi.repository.CctvCameraRepository;
import com.wd.custapi.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the stream-URL broker endpoint on CustomerCctvController.
 *
 * <p>Critical contract guards:
 * <ul>
 *   <li>Project membership is verified (via DashboardService) BEFORE the camera is
 *       fetched — a non-member's request must never reach the camera lookup.</li>
 *   <li>The response DTO contains ONLY cameraId, streamUrl, and protocol — credentials
 *       (username / password) are never present.</li>
 *   <li>If the stored streamUrl embeds "user:pass@" userinfo, it must be stripped
 *       before being returned to the client.</li>
 *   <li>Inactive cameras or cameras that do not belong to the requested project →
 *       {@link ResourceNotFoundException} (HTTP 404 via GlobalExceptionHandler).</li>
 *   <li>Non-member access → the membership guard throws, bubbling out before the
 *       camera repository is consulted (HTTP 404 via GlobalExceptionHandler, since
 *       DashboardService uses "Project not found or access denied").</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CustomerCctvStreamControllerTest {

    @Mock private CctvCameraRepository cameraRepository;
    @Mock private DashboardService dashboardService;
    @Mock private Authentication auth;

    @InjectMocks
    private CustomerCctvController controller;

    private static final String PROJECT_UUID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final Long PROJECT_ID = 5L;
    private static final Long CAMERA_ID = 42L;

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(PROJECT_ID);

        when(auth.getName()).thenReturn("member@example.com");
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenReturn(project);
    }

    // ─── Happy path ────────────────────────────────────────────────────────────

    @Test
    void memberGetsStreamDtoWithUrlAndProtocol() {
        CctvCamera camera = activeCameraWith("rtsp://192.168.1.10:554/live", StreamProtocol.RTSP);
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(CAMERA_ID, PROJECT_ID))
                .thenReturn(Optional.of(camera));

        ResponseEntity<CctvStreamDto> response = controller.getCameraStream(PROJECT_UUID, CAMERA_ID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        CctvStreamDto dto = response.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.cameraId()).isEqualTo(CAMERA_ID);
        assertThat(dto.streamUrl()).isEqualTo("rtsp://192.168.1.10:554/live");
        assertThat(dto.protocol()).isEqualTo(StreamProtocol.RTSP);
    }

    // ─── Credential stripping ──────────────────────────────────────────────────

    @Test
    void embeddedCredentialsInStoredUrlAreStrippedFromResponse() {
        // Camera's stored URL contains "admin:secret123@" — must not reach client.
        CctvCamera camera = activeCameraWith("rtsp://admin:secret123@192.168.1.10:554/live", StreamProtocol.RTSP);
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(CAMERA_ID, PROJECT_ID))
                .thenReturn(Optional.of(camera));

        ResponseEntity<CctvStreamDto> response = controller.getCameraStream(PROJECT_UUID, CAMERA_ID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        CctvStreamDto dto = response.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.streamUrl()).isEqualTo("rtsp://192.168.1.10:554/live");
        // Credentials must not appear anywhere in the returned URL.
        assertThat(dto.streamUrl()).doesNotContain("admin");
        assertThat(dto.streamUrl()).doesNotContain("secret123");
    }

    @Test
    void responseNeverContainsRawCredentials() {
        CctvCamera camera = cameraStub(CAMERA_ID, "http://user:pass@10.0.0.1:8080/video", StreamProtocol.HTTP);
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(CAMERA_ID, PROJECT_ID))
                .thenReturn(Optional.of(camera));

        ResponseEntity<CctvStreamDto> response = controller.getCameraStream(PROJECT_UUID, CAMERA_ID, auth);

        CctvStreamDto dto = response.getBody();
        assertThat(dto).isNotNull();
        // CctvStreamDto has no username/password fields — credentials cannot be present.
        assertThat(dto.streamUrl()).doesNotContain("user");
        assertThat(dto.streamUrl()).doesNotContain("pass");
        assertThat(dto.cameraId()).isEqualTo(CAMERA_ID);
        assertThat(dto.protocol()).isEqualTo(StreamProtocol.HTTP);
    }

    // ─── Not-found cases ────────────────────────────────────────────────────────

    @Test
    void unknownCamera_throwsResourceNotFoundException() {
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(CAMERA_ID, PROJECT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getCameraStream(PROJECT_UUID, CAMERA_ID, auth))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void inactiveCameraOrCameraNotInProject_throwsResourceNotFoundException() {
        // The repository method filters by projectId AND isActive=true; empty Optional
        // covers both "inactive" and "wrong project" cases at the controller level.
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getCameraStream(PROJECT_UUID, 99L, auth))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── Non-member access ─────────────────────────────────────────────────────

    @Test
    void nonMember_membershipGuardThrowsBeforeCameraLookup() {
        // DashboardService throws when the caller is not a project member.
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenThrow(new RuntimeException("Project not found or access denied"));

        assertThatThrownBy(() -> controller.getCameraStream(PROJECT_UUID, CAMERA_ID, auth))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("access denied");

        // The camera repository must NOT have been consulted — guard must fire first.
        org.mockito.Mockito.verifyNoInteractions(cameraRepository);
    }

    // ─── Ordering ──────────────────────────────────────────────────────────────

    @Test
    void membershipCheckRunsBeforeCameraLookup() {
        CctvCamera camera = activeCameraWith("rtsp://192.168.0.1:554/cam", StreamProtocol.RTSP);
        when(cameraRepository.findByIdAndProjectIdAndIsActiveTrue(CAMERA_ID, PROJECT_ID))
                .thenReturn(Optional.of(camera));

        controller.getCameraStream(PROJECT_UUID, CAMERA_ID, auth);

        InOrder ordered = inOrder(dashboardService, cameraRepository);
        ordered.verify(dashboardService).getProjectByUuidAndEmail(PROJECT_UUID, "member@example.com");
        ordered.verify(cameraRepository).findByIdAndProjectIdAndIsActiveTrue(CAMERA_ID, PROJECT_ID);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Creates a mock CctvCamera with the minimal stubs the controller actually calls:
     * {@code getId()}, {@code getStreamUrl()}, and {@code getStreamProtocol()}.
     * No credential stubs are added — the DTO has no such fields, so they would be
     * flagged as unnecessary by Mockito's strict stubbing.
     */
    private CctvCamera activeCameraWith(String streamUrl, StreamProtocol protocol) {
        return cameraStub(CAMERA_ID, streamUrl, protocol);
    }

    private CctvCamera cameraStub(Long id, String streamUrl, StreamProtocol protocol) {
        CctvCamera cam = mock(CctvCamera.class);
        when(cam.getId()).thenReturn(id);
        when(cam.getStreamUrl()).thenReturn(streamUrl);
        when(cam.getStreamProtocol()).thenReturn(protocol);
        return cam;
    }
}
