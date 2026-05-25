package com.wd.custapi.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Strips embedded credentials (userinfo) from stream URLs before they are
 * returned to the customer app.
 *
 * <p>Camera administrators sometimes store stream URLs in the form
 * {@code rtsp://admin:secret@host:port/path}. Returning such a URL verbatim
 * would leak the camera credentials to the client — a direct violation of the
 * security requirement documented in {@code CustomerCctvCameraDto}.
 *
 * <p>This helper removes the {@code user:password@} userinfo component using
 * {@link URI} parsing so that the returned URL is safe to expose.
 */
public final class StreamUrlSanitizer {

    private StreamUrlSanitizer() {}

    /**
     * Returns the URL with any embedded userinfo (credentials) removed.
     *
     * <ul>
     *   <li>{@code rtsp://admin:pass@host:554/live} → {@code rtsp://host:554/live}</li>
     *   <li>{@code rtsp://host:554/live} → {@code rtsp://host:554/live} (unchanged)</li>
     *   <li>{@code null} → {@code null}</li>
     * </ul>
     *
     * @param streamUrl raw URL (may contain userinfo)
     * @return URL with userinfo stripped, or the original string if parsing fails
     */
    public static String stripCredentials(String streamUrl) {
        if (streamUrl == null || streamUrl.isEmpty()) {
            return streamUrl;
        }
        try {
            URI original = new URI(streamUrl);
            if (original.getUserInfo() == null) {
                return streamUrl;
            }
            // Rebuild the URI without the userinfo component
            URI clean = new URI(
                    original.getScheme(),
                    null,                   // userInfo — explicitly null
                    original.getHost(),
                    original.getPort(),
                    original.getPath(),
                    original.getQuery(),
                    original.getFragment()
            );
            return clean.toString();
        } catch (URISyntaxException e) {
            // If the URL cannot be parsed (e.g. non-standard scheme), return as-is.
            // The caller should rely on the stored URL being free of credentials, but
            // we do not want to break playback for cameras using unusual URL formats.
            return streamUrl;
        }
    }
}
