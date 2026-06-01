package com.wd.custapi.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StreamUrlSanitizer.
 *
 * Critical contract: credentials embedded as "user:pass@" userinfo in a URL
 * MUST be stripped before the URL is returned to the customer app. A customer
 * must never receive raw camera credentials in any form.
 */
class StreamUrlSanitizerTest {

    @Test
    void plainUrl_isReturnedUnchanged() {
        String url = "rtsp://192.168.1.100:554/stream1";
        assertThat(StreamUrlSanitizer.stripCredentials(url)).isEqualTo(url);
    }

    @Test
    void hlsUrl_withNoCredentials_isReturnedUnchanged() {
        String url = "http://192.168.1.100:8080/hls/camera1.m3u8";
        assertThat(StreamUrlSanitizer.stripCredentials(url)).isEqualTo(url);
    }

    @Test
    void urlWithEmbeddedCredentials_hasUserinfoStripped() {
        // rtsp://admin:secret123@192.168.1.100:554/live → rtsp://192.168.1.100:554/live
        String url = "rtsp://admin:secret123@192.168.1.100:554/live";
        String sanitized = StreamUrlSanitizer.stripCredentials(url);

        assertThat(sanitized).isEqualTo("rtsp://192.168.1.100:554/live")
                .doesNotContain("admin")
                .doesNotContain("secret123");
    }

    @Test
    void urlWithUsernameOnlyNoPassword_hasUserinfoStripped() {
        // rtsp://admin@192.168.1.100:554/stream → rtsp://192.168.1.100:554/stream
        String url = "rtsp://admin@192.168.1.100:554/stream";
        String sanitized = StreamUrlSanitizer.stripCredentials(url);

        assertThat(sanitized).isEqualTo("rtsp://192.168.1.100:554/stream")
                .doesNotContain("admin");
    }

    @Test
    void urlWithSpecialCharsInPassword_hasUserinfoStripped() {
        String url = "rtsp://user:p%40ss!word@10.0.0.1:554/cam";
        String sanitized = StreamUrlSanitizer.stripCredentials(url);

        assertThat(sanitized).isEqualTo("rtsp://10.0.0.1:554/cam")
                .doesNotContain("user");
    }

    @Test
    void nullUrl_returnsNull() {
        assertThat(StreamUrlSanitizer.stripCredentials(null)).isNull();
    }

    @Test
    void blankUrl_returnsBlank() {
        assertThat(StreamUrlSanitizer.stripCredentials("")).isEmpty();
    }

    @Test
    void httpUrlWithCredentials_hasUserinfoStripped() {
        String url = "http://cam:pass@192.168.0.50:8080/video.mjpeg";
        String sanitized = StreamUrlSanitizer.stripCredentials(url);

        assertThat(sanitized).isEqualTo("http://192.168.0.50:8080/video.mjpeg")
                .doesNotContain("cam")
                .doesNotContain("pass");
    }
}
