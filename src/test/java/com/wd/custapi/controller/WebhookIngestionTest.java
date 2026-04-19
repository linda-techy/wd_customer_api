package com.wd.custapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.custapi.dto.PortalEventType;
import com.wd.custapi.dto.PortalWebhookEvent;
import com.wd.custapi.service.WebhookIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookIngestionTest {

    @Mock
    private WebhookIngestionService ingestionService;

    @InjectMocks
    private InternalWebhookController webhookController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SECRET = "test-webhook-secret";

    @BeforeEach
    void setUp() throws Exception {
        // Inject ObjectMapper (not a Spring context — inject manually)
        ReflectionTestUtils.setField(webhookController, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(webhookController, "webhookSecret", SECRET);
    }

    // ── Signature verification ────────────────────────────────────────────────

    @Test
    void receivePortalEvent_validSignature_acceptsEvent() throws Exception {
        String payload = buildPayload();
        String signature = "sha256=" + hmac(payload, SECRET);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Portal-Signature")).thenReturn(signature);

        ResponseEntity<Map<String, String>> response =
                webhookController.receivePortalEvent(payload, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("accepted", response.getBody().get("status"));
        verify(ingestionService).process(any(PortalWebhookEvent.class));
    }

    @Test
    void receivePortalEvent_invalidSignature_returns401() throws Exception {
        String payload = buildPayload();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Portal-Signature")).thenReturn("sha256=deadbeef00000000");

        ResponseEntity<Map<String, String>> response =
                webhookController.receivePortalEvent(payload, request);

        assertEquals(401, response.getStatusCode().value());
        verify(ingestionService, never()).process(any());
    }

    @Test
    void receivePortalEvent_missingSignatureHeader_returns401() throws Exception {
        String payload = buildPayload();

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Portal-Signature")).thenReturn(null);

        ResponseEntity<Map<String, String>> response =
                webhookController.receivePortalEvent(payload, request);

        assertEquals(401, response.getStatusCode().value());
        verify(ingestionService, never()).process(any());
    }

    // ── Event processing ──────────────────────────────────────────────────────

    @Test
    void receivePortalEvent_validPayload_passesCorrectEventToService() throws Exception {
        String payload = buildPayload();
        String signature = "sha256=" + hmac(payload, SECRET);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Portal-Signature")).thenReturn(signature);

        webhookController.receivePortalEvent(payload, request);

        ArgumentCaptor<PortalWebhookEvent> captor = ArgumentCaptor.forClass(PortalWebhookEvent.class);
        verify(ingestionService).process(captor.capture());

        PortalWebhookEvent captured = captor.getValue();
        assertEquals(PortalEventType.INVOICE_ISSUED, captured.eventType());
        assertEquals(42L, captured.projectId());
    }

    @Test
    void receivePortalEvent_malformedJson_returns400() throws Exception {
        String badPayload = "{ not valid json }";
        String signature = "sha256=" + hmac(badPayload, SECRET);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Portal-Signature")).thenReturn(signature);

        ResponseEntity<Map<String, String>> response =
                webhookController.receivePortalEvent(badPayload, request);

        assertEquals(400, response.getStatusCode().value());
        verify(ingestionService, never()).process(any());
    }

    @Test
    void receivePortalEvent_noSecretConfigured_acceptsWithoutSignatureCheck() throws Exception {
        // When secret is blank, signature is NOT verified
        ReflectionTestUtils.setField(webhookController, "webhookSecret", "");
        String payload = buildPayload();

        HttpServletRequest request = mock(HttpServletRequest.class);
        // No header — but should still accept because secret is not configured
        when(request.getHeader("X-Portal-Signature")).thenReturn(null);

        ResponseEntity<Map<String, String>> response =
                webhookController.receivePortalEvent(payload, request);

        assertEquals(200, response.getStatusCode().value());
        verify(ingestionService).process(any(PortalWebhookEvent.class));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildPayload() throws Exception {
        // Construct a minimal valid PortalWebhookEvent JSON
        return "{\"eventType\":\"INVOICE_ISSUED\",\"projectId\":42,\"referenceId\":7," +
               "\"customerId\":null,\"summary\":\"Invoice #001 issued\"," +
               "\"metadata\":{\"invoiceNumber\":\"INV-001\"},\"occurredAt\":null}";
    }

    private String hmac(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
