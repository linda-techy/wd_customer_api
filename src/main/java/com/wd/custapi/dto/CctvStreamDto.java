package com.wd.custapi.dto;

import com.wd.custapi.model.enums.StreamProtocol;

/**
 * Response DTO for the customer stream-URL broker endpoint.
 *
 * <p><b>Security invariant</b>: this record must NEVER include camera credentials
 * ({@code username}, {@code password}) or any URL that still contains embedded
 * userinfo. The controller is responsible for passing a sanitized URL via
 * {@link com.wd.custapi.util.StreamUrlSanitizer#stripCredentials}.
 */
public record CctvStreamDto(
        Long cameraId,
        String streamUrl,
        StreamProtocol protocol
) {}
