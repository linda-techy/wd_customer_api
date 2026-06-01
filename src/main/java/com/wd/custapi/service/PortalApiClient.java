package com.wd.custapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Outbound HMAC-signed client for portal-API /internal/** endpoints.
 * Mirrors WebhookPublisherService.computeSignature on portal-API: header
 * X-Portal-Signature: sha256=<hex>.
 */
@Service
public class PortalApiClient {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIG_HEADER = "X-Portal-Signature";

    @Value("${portal.api.url:http://localhost:8080}")
    private String portalApiUrl;

    @Value("${portal.internal-secret:${app.portal.webhook-secret:}}")
    private String sharedSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper json = new ObjectMapper();

    public PortalApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> requestCrOtp(Long crId, Long customerUserId) {
        Map<String, Object> body = Map.of("crId", crId, "customerUserId", customerUserId);
        return signedPost("/internal/cr-request-otp", body);
    }

    public Map<String, Object> approveCr(Long crId, Long customerUserId,
                                          String otpCode, String customerIp) {
        Map<String, Object> body = Map.of(
            "crId", crId,
            "customerUserId", customerUserId,
            "otpCode", otpCode,
            "customerIp", customerIp == null ? "" : customerIp);
        return signedPost("/internal/cr-approve", body);
    }

    /**
     * Resolve a CR to its owning portal projectId. Used by {@code CrAccessGuard}
     * to verify a customer owns the project of the CR they're trying to act on.
     *
     * @return the projectId of the CR's project
     * @throws IllegalStateException if portal returns no projectId
     *                                 (corrupt CR row, or 4xx from portal)
     */
    public Long fetchCrProjectId(Long crId) {
        Map<String, Object> result = signedPost("/internal/cr-project-id", Map.of("crId", crId));
        if (result == null) {
            throw new IllegalStateException("portal-API did not return projectId for crId=" + crId);
        }
        Object pid = result.get("projectId");
        if (pid instanceof Number n) return n.longValue();
        throw new IllegalStateException("portal-API did not return projectId for crId=" + crId);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, Object> signedPost(String path, Map<String, Object> body) {
        String jsonBody;
        try { jsonBody = this.json.writeValueAsString(body); }
        catch (Exception e) { throw new IllegalStateException("JSON serialise failed", e); }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(SIG_HEADER, "sha256=" + sign(jsonBody));

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                portalApiUrl + path, HttpMethod.POST, entity, Map.class);
            return resp.getBody() == null ? Map.of() : (Map<String, Object>) resp.getBody();
        } catch (HttpClientErrorException.TooManyRequests e) {
            long retry = parseRetryAfter(e.getResponseBodyAsString());
            throw new RateLimitedException(retry);
        }
    }

    private long parseRetryAfter(String body) {
        try {
            Map<?, ?> m = json.readValue(body, Map.class);
            Object v = m.get("retryAfterSeconds");
            if (v instanceof Number n) return n.longValue();
        } catch (Exception ignored) {
            // Malformed/empty 429 body — fall back to the default retry-after below.
        }
        return 3600L;
    }

    private String sign(String payload) {
        if (sharedSecret == null || sharedSecret.isBlank()) return "";
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC compute failed", e);
        }
    }

    public static class RateLimitedException extends RuntimeException {
        private final long retryAfterSeconds;
        public RateLimitedException(long retryAfterSeconds) {
            super("portal-API rate-limited OTP request");
            this.retryAfterSeconds = retryAfterSeconds;
        }
        public long getRetryAfterSeconds() { return retryAfterSeconds; }
    }
}
