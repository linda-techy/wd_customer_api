package com.wd.custapi.dto;

import com.wd.custapi.model.CctvCamera;
import com.wd.custapi.model.enums.StreamProtocol;

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
        String authedStreamUrl = buildAuthenticatedUrl(cam);
        return new CustomerCctvCameraDto(
                cam.getId(),
                cam.getCameraName(),
                cam.getLocation(),
                cam.getProvider(),
                cam.getStreamProtocol(),
                authedStreamUrl,
                cam.getSnapshotUrl(),
                cam.getResolution()
        );
    }

    private static String buildAuthenticatedUrl(CctvCamera cam) {
        String url = cam.getStreamUrl();
        if (url == null || url.isEmpty()) return url;
        String user = cam.getUsername();
        String pass = cam.getPassword();
        if (user == null || user.isEmpty() || pass == null || pass.isEmpty()) return url;

        // For RTSP: inject user:pass@host
        if (cam.getStreamProtocol() == StreamProtocol.RTSP && url.startsWith("rtsp://")) {
            return url.replace("rtsp://", "rtsp://" + user + ":" + pass + "@");
        }
        // For HLS/HTTP: append as query params
        if (url.contains("?")) {
            return url + "&user=" + user + "&password=" + pass;
        }
        return url + "?user=" + user + "&password=" + pass;
    }
}
