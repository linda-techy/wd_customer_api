package com.wd.custapi.service;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure-logic tests for the CCTV HLS reverse-proxy: manifest rewriting (segment /
 * variant / key URIs → proxy refs anchored to absolute upstream URLs), the SSRF
 * same-host guard on segment resolution, and server-side Basic-auth injection.
 */
class CctvStreamProxyServiceTest {

    private final CctvStreamProxyService svc = new CctvStreamProxyService();

    @Test
    void rewritesSegmentUrisToAbsoluteEncodedProxyRefs() {
        URI manifestUrl = URI.create("https://cam.example.com/live/stream.m3u8");
        String manifest = String.join("\n",
                "#EXTM3U",
                "#EXT-X-VERSION:3",
                "#EXT-X-TARGETDURATION:10",
                "#EXTINF:9.0,",
                "segment0.ts",
                "#EXTINF:9.0,",
                "720p/segment1.ts",
                "#EXT-X-ENDLIST");

        String out = svc.rewriteHlsManifest(manifest, manifestUrl);

        // Directives preserved verbatim
        assertThat(out).contains("#EXTM3U").contains("#EXT-X-TARGETDURATION:10")
                .contains("#EXTINF:9.0,").contains("#EXT-X-ENDLIST");
        // Relative segment refs resolved to ABSOLUTE upstream URLs, then encoded behind the proxy
        assertThat(out).contains("segment?s="
                + CctvStreamProxyService.encodeRef("https://cam.example.com/live/segment0.ts"));
        assertThat(out).contains("segment?s="
                + CctvStreamProxyService.encodeRef("https://cam.example.com/live/720p/segment1.ts"));
        // Raw upstream paths must not leak into the manifest
        assertThat(out).doesNotContain("720p/segment1.ts");
    }

    @Test
    void rewritesEncryptionKeyAndMapUris() {
        URI manifestUrl = URI.create("https://cam.example.com/live/stream.m3u8");
        String manifest = String.join("\n",
                "#EXTM3U",
                "#EXT-X-KEY:METHOD=AES-128,URI=\"key.bin\",IV=0x0",
                "#EXTINF:9.0,",
                "seg.ts");

        String out = svc.rewriteHlsManifest(manifest, manifestUrl);

        assertThat(out).contains("URI=\"segment?s="
                + CctvStreamProxyService.encodeRef("https://cam.example.com/live/key.bin") + "\"");
        assertThat(out).doesNotContain("URI=\"key.bin\"");
    }

    @Test
    void resolveUpstreamSegment_returnsAbsoluteUrlOnSameHost() {
        String cameraUrl = "https://cam.example.com/live/stream.m3u8";
        String token = CctvStreamProxyService.encodeRef("https://cam.example.com/live/segment0.ts");

        URI upstream = svc.resolveUpstreamSegment(cameraUrl, token);

        assertThat(upstream.toString()).isEqualTo("https://cam.example.com/live/segment0.ts");
    }

    @Test
    void resolveUpstreamSegment_rejectsDifferentHost_ssrfGuard() {
        String cameraUrl = "https://cam.example.com/live/stream.m3u8";
        // Classic SSRF target — cloud metadata endpoint on a different host
        String token = CctvStreamProxyService.encodeRef("http://169.254.169.254/latest/meta-data/");

        assertThatThrownBy(() -> svc.resolveUpstreamSegment(cameraUrl, token))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void resolveUpstreamSegment_rejectsNonHttpScheme() {
        String cameraUrl = "https://cam.example.com/live/stream.m3u8";
        String token = CctvStreamProxyService.encodeRef("file:///etc/passwd");

        assertThatThrownBy(() -> svc.resolveUpstreamSegment(cameraUrl, token))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void basicAuthHeader_builtFromCredentials() {
        String expected = "Basic " + Base64.getEncoder().encodeToString("admin:s3cr3t".getBytes());
        assertThat(svc.basicAuthHeader("admin", "s3cr3t")).isEqualTo(expected);
    }

    @Test
    void basicAuthHeader_nullWhenNoUsername() {
        assertThat(svc.basicAuthHeader(null, "x")).isNull();
        assertThat(svc.basicAuthHeader("", "x")).isNull();
    }
}
