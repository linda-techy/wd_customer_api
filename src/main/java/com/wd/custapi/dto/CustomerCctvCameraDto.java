package com.wd.custapi.dto;

import com.wd.custapi.model.CctvCamera;
import com.wd.custapi.model.enums.StreamProtocol;
import com.wd.custapi.util.StreamUrlSanitizer;

/**
 * Customer-safe CCTV camera projection.
 *
 * SECURITY: The camera's username/password MUST NOT be embedded in the returned
 * stream URL. A customer would then be able to read the credentials straight out
 * of the JSON response and authenticate to the camera directly, bypassing any
 * server-side auditing, rate limiting, or access revocation.
 *
 * This DTO returns the bare stream URL only. For playback to work, the customer
 * app streams through a backend proxy endpoint that injects credentials
 * server-side (see CustomerCctvController: /api/customer/projects/{projectId}/cctv-cameras/{cameraId}/stream).
 * Returning the bare URL here is intentional and preferable to leaking credentials.
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
                StreamUrlSanitizer.stripCredentials(cam.getStreamUrl()),  // credentials stripped
                cam.getSnapshotUrl(),
                cam.getResolution()
        );
    }
}
