package com.wd.custapi.dto;

import com.wd.custapi.model.CctvCamera;
import com.wd.custapi.model.enums.StreamProtocol;

/**
 * Customer-safe CCTV camera projection.
 *
 * SECURITY: The camera's username/password MUST NOT be embedded in the returned
 * stream URL. A customer would then be able to read the credentials straight out
 * of the JSON response and authenticate to the camera directly, bypassing any
 * server-side auditing, rate limiting, or access revocation.
 *
 * This DTO returns the bare stream URL only. For playback to work, the customer
 * app must stream through a backend proxy endpoint that injects credentials
 * server-side (TODO: implement /api/projects/{projectId}/cctv/{cameraId}/stream).
 * Until that proxy exists, direct playback may fail — this is intentional and
 * preferable to leaking credentials.
 */
public record CustomerCctvCameraDto(
        Long id,
        String cameraName,
        String location,
        String provider,
        StreamProtocol streamProtocol,
        String streamUrl,
        String snapshotUrl,
        String resolution
) {
    public static CustomerCctvCameraDto from(CctvCamera cam) {
        return new CustomerCctvCameraDto(
                cam.getId(),
                cam.getCameraName(),
                cam.getLocation(),
                cam.getProvider(),
                cam.getStreamProtocol(),
                cam.getStreamUrl(),       // bare URL, no credentials
                cam.getSnapshotUrl(),
                cam.getResolution()
        );
    }
}
