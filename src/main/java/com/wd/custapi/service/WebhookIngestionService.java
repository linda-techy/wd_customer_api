package com.wd.custapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.custapi.dto.PortalEventType;
import com.wd.custapi.dto.PortalWebhookEvent;
import com.wd.custapi.model.CustomerNotification;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.ReceivedWebhookEvent;
import com.wd.custapi.repository.CustomerNotificationRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.ReceivedWebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Processes inbound Portal API webhook events and converts them into
 * CustomerNotification records + Firebase push notifications.
 *
 * All processing is async so the HTTP response to the Portal API
 * returns immediately (decoupled from processing latency).
 *
 * Events are tracked in {@code received_webhook_events}. Failed events
 * are retried by {@link #retryFailedEvents()} every 10 minutes (max 3 attempts).
 */
@Service
public class WebhookIngestionService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestionService.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final CustomerNotificationRepository notificationRepository;
    private final CustomerUserRepository userRepository;
    private final PushNotificationService pushNotificationService;
    private final ReceivedWebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    public WebhookIngestionService(CustomerNotificationRepository notificationRepository,
                                   CustomerUserRepository userRepository,
                                   PushNotificationService pushNotificationService,
                                   ReceivedWebhookEventRepository webhookEventRepository,
                                   ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.pushNotificationService = pushNotificationService;
        this.webhookEventRepository = webhookEventRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional
    public void process(PortalWebhookEvent event) {
        // Persist event record with PROCESSING status before any work
        ReceivedWebhookEvent webhookRecord = new ReceivedWebhookEvent();
        webhookRecord.setEventType(event.eventType() != null ? event.eventType().name() : "UNKNOWN");
        webhookRecord.setStatus(ReceivedWebhookEvent.STATUS_PROCESSING);
        try {
            webhookRecord.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            webhookRecord.setPayload("{}");
        }
        webhookRecord = webhookEventRepository.save(webhookRecord);

        try {
            log.info("Processing portal webhook event: type={} projectId={} referenceId={}",
                    event.eventType(), event.projectId(), event.referenceId());

            doProcess(event);

            webhookRecord.setStatus(ReceivedWebhookEvent.STATUS_PROCESSED);
            webhookRecord.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookRecord);

        } catch (Exception e) {
            // Never propagate — webhook processing must not affect portal API response
            log.error("Error processing portal webhook event type={}: {}", event.eventType(), e.getMessage(), e);
            webhookRecord.setStatus(ReceivedWebhookEvent.STATUS_FAILED);
            webhookRecord.setErrorMessage(e.getMessage());
            webhookEventRepository.save(webhookRecord);
        }
    }

    /**
     * Retries FAILED webhook events every 10 minutes.
     * Abandons events that have already been attempted {@value MAX_RETRY_ATTEMPTS} times
     * by marking them FAILED permanently (error message is prefixed with "[ABANDONED]").
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void retryFailedEvents() {
        List<ReceivedWebhookEvent> failedEvents =
                webhookEventRepository.findByStatus(ReceivedWebhookEvent.STATUS_FAILED);

        if (failedEvents.isEmpty()) return;

        log.info("Retrying {} failed webhook event(s)", failedEvents.size());

        for (ReceivedWebhookEvent failedRecord : failedEvents) {
            int attempt = failedRecord.getAttemptCount() + 1;

            if (attempt > MAX_RETRY_ATTEMPTS) {
                log.warn("Abandoning webhook event id={} type={} after {} attempts",
                        failedRecord.getId(), failedRecord.getEventType(), MAX_RETRY_ATTEMPTS);
                failedRecord.setErrorMessage("[ABANDONED after " + MAX_RETRY_ATTEMPTS + " attempts] " + failedRecord.getErrorMessage());
                webhookEventRepository.save(failedRecord);
                continue;
            }

            failedRecord.setAttemptCount(attempt);
            failedRecord.setStatus(ReceivedWebhookEvent.STATUS_PROCESSING);
            webhookEventRepository.save(failedRecord);

            try {
                PortalWebhookEvent event = objectMapper.readValue(failedRecord.getPayload(), PortalWebhookEvent.class);
                doProcess(event);

                failedRecord.setStatus(ReceivedWebhookEvent.STATUS_PROCESSED);
                failedRecord.setProcessedAt(LocalDateTime.now());
                log.info("Successfully retried webhook event id={} type={} on attempt {}",
                        failedRecord.getId(), failedRecord.getEventType(), attempt);
            } catch (Exception e) {
                log.error("Retry attempt {} failed for webhook event id={} type={}: {}",
                        attempt, failedRecord.getId(), failedRecord.getEventType(), e.getMessage(), e);
                failedRecord.setStatus(ReceivedWebhookEvent.STATUS_FAILED);
                failedRecord.setErrorMessage(e.getMessage());
            }

            webhookEventRepository.save(failedRecord);
        }
    }

    // ── Core processing logic ────────────────────────────────────────────────

    private void doProcess(PortalWebhookEvent event) {
        String title = resolveTitle(event.eventType(), event.metadata());
        String body  = resolveBody(event, title);
        String notifType = resolveNotifType(event.eventType());

        List<CustomerUser> recipients = resolveRecipients(event);
        for (CustomerUser user : recipients) {
            saveNotification(user, event.projectId(), event.referenceId(), title, body, notifType);
            pushIfTokenPresent(user, title, body, event.eventType(), event.referenceId(), notifType);
        }
    }

    /**
     * Body resolution. For PAYMENT_MILESTONE_DUE we render server-side using
     * ContractValueFormatter (₹X L / ₹Y Cr / Indian-grouped) — the portal-API
     * webhook only carries the raw plain-string amount, never a pre-formatted
     * one. For all other event types the historical behaviour stands: use
     * {@code event.summary()} verbatim, falling back to the title.
     */
    private String resolveBody(PortalWebhookEvent event, String title) {
        if (event.eventType() == PortalEventType.PAYMENT_MILESTONE_DUE) {
            String stageNumber = getOrDefault(event.metadata(), "stageNumber", "?");
            String stageName   = getOrDefault(event.metadata(), "stageName", "");
            String amountRaw   = getOrDefault(event.metadata(), "netPayableAmount", "0");
            String formatted;
            try {
                formatted = com.wd.custapi.util.ContractValueFormatter.formatINR(new java.math.BigDecimal(amountRaw));
            } catch (IllegalArgumentException ex) {
                // Catches both NumberFormatException (BigDecimal parse) and the
                // formatter's own negative-amount IllegalArgumentException.
                log.warn("PAYMENT_MILESTONE_DUE: bad amount '{}' — falling back to raw", amountRaw);
                formatted = "\u20B9" + amountRaw;
            }
            return "Stage " + stageNumber + " \u2014 " + stageName + " (" + formatted + ")";
        }
        return event.summary() != null ? event.summary() : title;
    }

    private List<CustomerUser> resolveRecipients(PortalWebhookEvent event) {
        if (event.customerId() != null) {
            return userRepository.findById(event.customerId()).map(List::of).orElse(List.of());
        }
        if (event.projectId() != null) {
            // Notify all CUSTOMER and CUSTOMER_ADMIN members of the project
            return userRepository.findCustomersByProjectId(event.projectId());
        }
        return List.of();
    }

    private void saveNotification(CustomerUser user, Long projectId, Long referenceId,
                                   String title, String body, String notifType) {
        CustomerNotification notification = new CustomerNotification();
        notification.setCustomerUser(user);
        notification.setProjectId(projectId);
        notification.setReferenceId(referenceId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setNotificationType(notifType);
        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @SuppressWarnings("java:S125") // explanatory prose comments, not commented-out code
    private void pushIfTokenPresent(CustomerUser user, String title, String body,
                                     PortalEventType eventType, Long referenceId,
                                     String resolvedNotifType) {
        if (user.getFcmToken() == null || user.getFcmToken().isBlank()) return;
        java.util.Map<String, String> data = new java.util.HashMap<>();
        // 'type' = the customer-app's switch key in NotificationService._handleTap.
        // For PAYMENT_MILESTONE_DUE we want the customer-app to match a literal
        // "PAYMENT_MILESTONE_DUE", so we use the event-type name when available;
        // null guards against unknown future types.
        data.put("type", eventType != null ? eventType.name() : "UNKNOWN");
        data.put("notificationType", resolvedNotifType);
        data.put("referenceId", referenceId != null ? referenceId.toString() : "");
        if (eventType == PortalEventType.PAYMENT_MILESTONE_DUE) {
            data.put("deepLink", "payments");
        }
        pushNotificationService.sendToToken(user.getFcmToken(), title, body, data);
    }

    /**
     * Resolves a notification title for the given event type.
     *
     * <p>The {@code null, default} arm is forward-compat insurance: if portal-API
     * ships a new {@link PortalEventType} value before customer-API is rebuilt,
     * the inbound JSON will deserialise to a record with {@code eventType=null}
     * (Jackson's default for unknown enum names with appropriate config) — or a
     * future enum value the runtime classfile doesn't know. Falling through to
     * a generic "Project update" keeps the row PROCESSED rather than letting it
     * cycle through retries and end ABANDONED.
     */
    private String resolveTitle(PortalEventType type, Map<String, String> meta) {
        return switch (type) {
            case INVOICE_ISSUED   -> "New Invoice: " + getOrDefault(meta, "invoiceNumber", "");
            case INVOICE_PAID     -> "Invoice Paid: " + getOrDefault(meta, "invoiceNumber", "");
            case PHASE_UPDATED    -> "Phase Update: " + getOrDefault(meta, "phaseName", "Construction phase updated");
            case MILESTONE_REACHED -> "Milestone Reached: " + getOrDefault(meta, "milestoneName", "");
            case SITE_REPORT_SUBMITTED -> "New Site Report: " + getOrDefault(meta, "reportTitle", "");
            case DOCUMENT_UPLOADED -> "Document Added: " + getOrDefault(meta, "filename", "");
            case PAYMENT_RECORDED  -> "Payment Recorded: ₹" + getOrDefault(meta, "amount", "");
            case DELAY_REPORTED    -> "Delay Reported: " + getOrDefault(meta, "category", "");
            case HANDOVER_SHIFT    -> "Expected Handover Shifted";
            case PAYMENT_MILESTONE_DUE -> switch (getOrDefault(meta, "reminderKind", "")) {
                case "T_MINUS_3" -> "Payment due in 3 days";
                case "DUE_TODAY" -> "Payment due today";
                case "OVERDUE"   -> "Payment overdue";
                default          -> "Payment reminder";
            };
            case null, default     -> "Project update";
        };
    }

    /**
     * Resolves the customer-notification type bucket for the given event type.
     *
     * <p>Same forward-compat rationale as {@link #resolveTitle}: unknown / future
     * event types fall back to {@code "GENERAL"} so the notification record is
     * still persisted and pushed.
     */
    private String resolveNotifType(PortalEventType type) {
        return switch (type) {
            case INVOICE_ISSUED, INVOICE_PAID, PAYMENT_RECORDED -> "PAYMENT";
            case PHASE_UPDATED, MILESTONE_REACHED -> "MILESTONE";
            case SITE_REPORT_SUBMITTED -> "SITE_REPORT";
            case DOCUMENT_UPLOADED -> "DOCUMENT";
            case DELAY_REPORTED -> "DELAY";
            case HANDOVER_SHIFT -> "SCHEDULE";
            case PAYMENT_MILESTONE_DUE -> "PAYMENT_MILESTONE_DUE";
            case null, default -> "GENERAL";
        };
    }

    private String getOrDefault(Map<String, String> meta, String key, String def) {
        if (meta == null) return def;
        return meta.getOrDefault(key, def);
    }
}
