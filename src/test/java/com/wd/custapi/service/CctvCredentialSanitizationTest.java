package com.wd.custapi.service;

import com.wd.custapi.dto.CustomerCctvCameraDto;
import com.wd.custapi.dto.ProjectModuleDtos.CctvCameraDto;
import com.wd.custapi.model.CctvCamera;
import com.wd.custapi.model.enums.StreamProtocol;
import com.wd.custapi.repository.CctvCameraRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that CCTV stream URLs have embedded credentials stripped before
 * being returned to the customer.
 *
 * Two exposure surfaces are tested:
 * <ol>
 *   <li>{@link CustomerCctvCameraDto#from(CctvCamera)} — the static DTO factory.</li>
 *   <li>{@link CctvService#getActiveCameras(Long)} — the service method that maps
 *       {@code CctvCamera} entities via its private {@code toDto} helper.</li>
 * </ol>
 *
 * A URL of the form {@code rtsp://admin:secret@10.0.0.5:554/live} MUST be
 * returned as {@code rtsp://10.0.0.5:554/live} — no credentials present.
 *
 * Reference: audit Card 4.10
 */
@ExtendWith(MockitoExtension.class)
class CctvCredentialSanitizationTest {

    private static final String CREDENTIAL_URL = "rtsp://admin:secret@10.0.0.5:554/live";
    private static final String SANITIZED_URL   = "rtsp://10.0.0.5:554/live";

    @Mock private CctvCameraRepository cctvRepository;

    @InjectMocks
    private CctvService cctvService;

    // ─── CustomerCctvCameraDto.from(cam) ────────────────────────────────────────

    @Test
    void customerCctvCameraDto_from_stripsCredentialsFromStreamUrl() {
        CctvCamera cam = cameraStub(CREDENTIAL_URL);

        CustomerCctvCameraDto dto = CustomerCctvCameraDto.from(cam);

        assertThat(dto.streamUrl())
                .as("streamUrl must not contain credentials")
                .doesNotContain("admin")
                .doesNotContain("secret")
                .doesNotContain("admin:secret@");
    }

    @Test
    void customerCctvCameraDto_from_preservesHostAndPath() {
        CctvCamera cam = cameraStub(CREDENTIAL_URL);

        CustomerCctvCameraDto dto = CustomerCctvCameraDto.from(cam);

        assertThat(dto.streamUrl())
                .as("host and path must be preserved after stripping credentials")
                .isEqualTo(SANITIZED_URL);
    }

    @Test
    void customerCctvCameraDto_from_plainUrl_isUnchanged() {
        CctvCamera cam = cameraStub("rtsp://10.0.0.5:554/live");

        CustomerCctvCameraDto dto = CustomerCctvCameraDto.from(cam);

        assertThat(dto.streamUrl()).isEqualTo("rtsp://10.0.0.5:554/live");
    }

    // ─── CctvService.getActiveCameras (via toDto) ───────────────────────────────

    @Test
    void cctvService_getActiveCameras_stripsCredentialsFromReturnedDto() {
        CctvCamera cam = fullCameraStub(1L, 7L, CREDENTIAL_URL);
        when(cctvRepository.findByProjectIdAndIsActiveTrueOrderByDisplayOrder(7L))
                .thenReturn(List.of(cam));

        List<CctvCameraDto> dtos = cctvService.getActiveCameras(7L);

        assertThat(dtos).hasSize(1);
        CctvCameraDto dto = dtos.get(0);
        assertThat(dto.streamUrl())
                .as("CctvService.toDto must strip credentials")
                .doesNotContain("admin")
                .doesNotContain("secret")
                .doesNotContain("admin:secret@")
                .isEqualTo(SANITIZED_URL);
    }

    @Test
    void cctvService_getActiveCameras_plainUrl_isUnchanged() {
        String plain = "rtsp://10.0.0.5:554/live";
        CctvCamera cam = fullCameraStub(2L, 7L, plain);
        when(cctvRepository.findByProjectIdAndIsActiveTrueOrderByDisplayOrder(7L))
                .thenReturn(List.of(cam));

        List<CctvCameraDto> dtos = cctvService.getActiveCameras(7L);

        assertThat(dtos.get(0).streamUrl()).isEqualTo(plain);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /** Minimal stub for CustomerCctvCameraDto.from() — only streamUrl is required. */
    private CctvCamera cameraStub(String streamUrl) {
        CctvCamera cam = mock(CctvCamera.class);
        when(cam.getStreamUrl()).thenReturn(streamUrl);
        return cam;
    }

    /**
     * Full stub for CctvService.toDto() — all fields the private method accesses
     * must be stubbed to avoid Mockito "unnecessary stubbing" failures.
     */
    private CctvCamera fullCameraStub(Long id, Long projectId, String streamUrl) {
        CctvCamera cam = mock(CctvCamera.class);
        when(cam.getId()).thenReturn(id);
        when(cam.getProjectId()).thenReturn(projectId);
        when(cam.getCameraName()).thenReturn("Cam A");
        when(cam.getLocation()).thenReturn("Gate");
        when(cam.getProvider()).thenReturn("Hikvision");
        when(cam.getStreamProtocol()).thenReturn(StreamProtocol.RTSP);
        when(cam.getStreamUrl()).thenReturn(streamUrl);
        when(cam.getSnapshotUrl()).thenReturn(null);
        when(cam.getIsActive()).thenReturn(true);
        when(cam.getInstallationDate()).thenReturn(null);
        when(cam.getResolution()).thenReturn("1080p");
        when(cam.getDisplayOrder()).thenReturn(1);
        return cam;
    }
}
