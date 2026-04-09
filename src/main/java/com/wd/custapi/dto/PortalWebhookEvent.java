package com.wd.custapi.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Inbound webhook payload from the Portal API.
 * Signed with HMAC-SHA256; verified by WebhookSecurityFilter.
 */
public record PortalWebhookEvent(
    PortalEventType eventType,
    Long projectId,
    Long customerId,         // nullable — if null, notify all project owners
    Long referenceId,        // ID of the affected entity (invoice id, phase id, etc.)
    String summary,          // Human-readable one-liner for the notification
    Map<String, String> metadata,
    LocalDateTime occurredAt
) {}
