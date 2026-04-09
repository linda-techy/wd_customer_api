package com.wd.custapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.custapi.dto.PortalWebhookEvent;
import com.wd.custapi.service.WebhookIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Internal endpoint for receiving webhook events from the Portal API.
 *
 * Security: requests are HMAC-SHA256 signed. The shared secret is set via
 * the PORTAL_WEBHOOK_SECRET env variable.  IP allowlisting should additionally
 * be configured at the load-balancer/firewall level in production.
 *
 * This endpoint is excluded from JWT authentication (SecurityConfig permits /internal/**).
 */
@RestController
@RequestMapping("/internal")
public class InternalWebhookController {

    private static final Logger log = LoggerFactory.getLogger(InternalWebhookController.class);
    private static final String HMAC_HEADER = "X-Portal-Signature";
    private static final String ALGORITHM = "HmacSHA256";

    @Value("${app.portal.webhook-secret:}")
    private String webhookSecret;

    private final WebhookIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    public InternalWebhookController(WebhookIngestionService ingestionService,
                                      ObjectMapper objectMapper) {
        this.ingestionService = ingestionService;
        this.objectMapper = objectMapper;
    }

    /**
     * POST /internal/portal-events
     * Body: PortalWebhookEvent JSON
     * Header: X-Portal-Signature: sha256={hmac_hex}
     */
    @PostMapping("/portal-events")
    public ResponseEntity<Map<String, String>> receivePortalEvent(
            @RequestBody String rawBody,
            HttpServletRequest request) {

        // Verify HMAC signature when a secret is configured
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            String signature = request.getHeader(HMAC_HEADER);
            if (!verifySignature(rawBody, signature)) {
                log.warn("Webhook received with invalid signature — rejected");
                return ResponseEntity.status(401).body(Map.of("error", "Invalid signature"));
            }
        }

        try {
            PortalWebhookEvent event = objectMapper.readValue(rawBody, PortalWebhookEvent.class);
            ingestionService.process(event); // Async — returns immediately
            return ResponseEntity.ok(Map.of("status", "accepted"));
        } catch (Exception e) {
            log.error("Failed to parse portal webhook event: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload: " + e.getMessage()));
        }
    }

    private boolean verifySignature(String payload, String signatureHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) return false;
        try {
            // Header format: "sha256=<hex>"
            String expected = signatureHeader.startsWith("sha256=")
                    ? signatureHeader.substring(7) : signatureHeader;

            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equalsIgnoreCase(expected);
        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}
