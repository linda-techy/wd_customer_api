package com.wd.custapi.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * Server-side HLS reverse-proxy for customer CCTV playback.
 *
 * <p>The customer must never receive the camera host or its credentials. This
 * service fetches the camera's HLS manifest/segments server-side (injecting the
 * stored Basic-auth credentials) and rewrites the manifest so every segment,
 * variant playlist and key URI is routed back through the proxy. The customer's
 * player only ever talks to the customer-API.
 *
 * <p>Each rewritten reference encodes the <b>absolute</b> upstream URL (resolved
 * against the manifest it appeared in), so master→media→segment nesting resolves
 * correctly and {@link #resolveUpstreamSegment} can enforce an SSRF guard: a
 * proxied fetch is only allowed to the camera's own host over http(s).
 *
 * <p>RTSP and other non-HTTP cameras cannot be proxied here (browsers can't play
 * RTSP); those need a transcoder and are handled as 501 by the controller.
 */
@Service
public class CctvStreamProxyService {

    /** Relative path of the segment-proxy endpoint, as referenced from /stream.m3u8. */
    static final String SEGMENT_ENDPOINT = "segment";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // ── Manifest rewriting ──────────────────────────────────────────────────

    /**
     * Rewrites an HLS manifest so every URI (media segment, variant playlist,
     * EXT-X-KEY / EXT-X-MAP) is replaced with {@code segment?s=<token>}, where the
     * token encodes the absolute upstream URL resolved against {@code manifestUrl}.
     */
    public String rewriteHlsManifest(String manifest, URI manifestUrl) {
        if (manifest == null) {
            return null;
        }
        String[] lines = manifest.split("\n", -1);
        StringBuilder out = new StringBuilder(manifest.length() + 256);
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                out.append(raw);
            } else if (trimmed.startsWith("#")) {
                out.append(rewriteDirectiveUri(trimmed, manifestUrl));
            } else {
                out.append(toProxyRef(trimmed, manifestUrl));
            }
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }
        return out.toString();
    }

    /** Rewrites a {@code URI="..."} attribute (EXT-X-KEY / EXT-X-MAP) if present. */
    private String rewriteDirectiveUri(String line, URI manifestUrl) {
        int idx = line.indexOf("URI=\"");
        if (idx < 0) {
            return line;
        }
        int start = idx + 5;
        int end = line.indexOf('"', start);
        if (end < 0) {
            return line;
        }
        String uri = line.substring(start, end);
        return line.substring(0, start) + toProxyRef(uri, manifestUrl) + line.substring(end);
    }

    private String toProxyRef(String ref, URI manifestUrl) {
        String absolute = manifestUrl.resolve(ref).toString();
        return SEGMENT_ENDPOINT + "?s=" + encodeRef(absolute);
    }

    public static String encodeRef(String ref) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(ref.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeRef(String token) {
        return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
    }

    // ── SSRF-guarded segment resolution ─────────────────────────────────────

    /**
     * Decodes a proxy token into the absolute upstream URL and verifies it is a
     * safe fetch target: http/https only, and the SAME host as the camera's
     * stream URL. Throws {@link SecurityException} otherwise (SSRF guard).
     */
    public URI resolveUpstreamSegment(String cameraStreamUrl, String token) {
        URI cameraUri = URI.create(cameraStreamUrl);
        URI target;
        try {
            target = URI.create(decodeRef(token));
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Malformed proxied segment reference");
        }
        String scheme = target.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new SecurityException("Disallowed scheme for proxied segment: " + scheme);
        }
        if (cameraUri.getHost() == null || target.getHost() == null
                || !cameraUri.getHost().equalsIgnoreCase(target.getHost())) {
            throw new SecurityException("Proxied segment host does not match the camera stream host");
        }
        return target;
    }

    // ── Credential injection ────────────────────────────────────────────────

    /** Basic-auth header from stored camera credentials, or {@code null} if none. */
    public String basicAuthHeader(String username, String password) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        String creds = username + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
    }

    // ── Upstream fetch (relay) ──────────────────────────────────────────────

    /** Result of a proxied upstream fetch. */
    public record UpstreamResponse(int status, String contentType, byte[] body) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UpstreamResponse other)) return false;
            return status == other.status
                    && java.util.Objects.equals(contentType, other.contentType)
                    && java.util.Arrays.equals(body, other.body);
        }

        @Override
        public int hashCode() {
            return 31 * java.util.Objects.hash(status, contentType) + java.util.Arrays.hashCode(body);
        }

        @Override
        public String toString() {
            return "UpstreamResponse[status=" + status
                    + ", contentType=" + contentType
                    + ", body=" + java.util.Arrays.toString(body) + "]";
        }
    }

    /**
     * Fetches an upstream URL server-side, optionally with a Basic-auth header.
     * Used by the controller to relay manifests and segments to the customer.
     */
    public UpstreamResponse fetch(URI url, String authHeader) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(url)
                .timeout(Duration.ofSeconds(15))
                .GET();
        if (authHeader != null) {
            req.header("Authorization", authHeader);
        }
        HttpResponse<byte[]> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofByteArray());
        String ct = resp.headers().firstValue("content-type").orElse("application/octet-stream");
        return new UpstreamResponse(resp.statusCode(), ct, resp.body());
    }

    /** Heuristic: does this content look like an HLS manifest (so it needs rewriting)? */
    public boolean isHlsManifest(String contentType, byte[] body) {
        if (contentType != null
                && (contentType.contains("mpegurl") || contentType.contains("vnd.apple.mpegurl"))) {
            return true;
        }
        if (body != null && body.length >= 7) {
            String head = new String(body, 0, Math.min(body.length, 16), StandardCharsets.UTF_8);
            return head.startsWith("#EXTM3U");
        }
        return false;
    }
}
