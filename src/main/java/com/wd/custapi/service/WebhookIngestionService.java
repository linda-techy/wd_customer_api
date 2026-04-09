package com.wd.custapi.service;

import com.wd.custapi.dto.PortalEventType;
import com.wd.custapi.dto.PortalWebhookEvent;
import com.wd.custapi.model.CustomerNotification;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.repository.CustomerNotificationRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Processes inbound Portal API webhook events and converts them into
 * CustomerNotification records + Firebase push notifications.
 *
 * All processing is async so the HTTP response to the Portal API
 * returns immediately (decoupled from processing latency).
 */
@Service
public class WebhookIngestionService {

    private static final Logger log = LoggerFactory.getLogger(WebhookIngestionService.class);

    private final CustomerNotificationRepository notificationRepository;
    private final CustomerUserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    public WebhookIngestionService(CustomerNotificationRepository notificationRepository,
                                   CustomerUserRepository userRepository,
                                   PushNotificationService pushNotificationService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.pushNotificationService = pushNotificationService;
    }

    @Async
    @Transactional
    public void process(PortalWebhookEvent event) {
        try {
            log.info("Processing portal webhook event: type={} projectId={} referenceId={}",
                    event.eventType(), event.projectId(), event.referenceId());

            String title = resolveTitle(event.eventType(), event.metadata());
            String body = event.summary() != null ? event.summary() : title;
            String notifType = resolveNotifType(event.eventType());

            List<CustomerUser> recipients = resolveRecipients(event);
            for (CustomerUser user : recipients) {
                saveNotification(user, event.projectId(), event.referenceId(), title, body, notifType);
                pushIfTokenPresent(user, title, body, event.eventType(), event.referenceId());
            }
        } catch (Exception e) {
            // Never propagate — webhook processing must not affect portal API response
            log.error("Error processing portal webhook event type={}: {}", event.eventType(), e.getMessage(), e);
        }
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

    private void pushIfTokenPresent(CustomerUser user, String title, String body,
                                     PortalEventType eventType, Long referenceId) {
        if (user.getFcmToken() == null || user.getFcmToken().isBlank()) return;
        pushNotificationService.sendToToken(
                user.getFcmToken(), title, body,
                Map.of("notificationType", resolveNotifType(eventType),
                       "referenceId", referenceId != null ? referenceId.toString() : ""));
    }

    private String resolveTitle(PortalEventType type, Map<String, String> meta) {
        return switch (type) {
            case INVOICE_ISSUED   -> "New Invoice: " + getOrDefault(meta, "invoiceNumber", "");
            case INVOICE_PAID     -> "Invoice Paid: " + getOrDefault(meta, "invoiceNumber", "");
            case PHASE_UPDATED    -> "Phase Update: " + getOrDefault(meta, "phaseName", "Construction phase updated");
            case MILESTONE_REACHED -> "Milestone Reached: " + getOrDefault(meta, "milestoneName", "");
            case SITE_REPORT_SUBMITTED -> "New Site Report: " + getOrDefault(meta, "reportTitle", "");
            case DOCUMENT_UPLOADED -> "Document Added: " + getOrDefault(meta, "filename", "");
            case PAYMENT_RECORDED  -> "Payment Recorded: ₹" + getOrDefault(meta, "amount", "");
        };
    }

    private String resolveNotifType(PortalEventType type) {
        return switch (type) {
            case INVOICE_ISSUED, INVOICE_PAID, PAYMENT_RECORDED -> "PAYMENT";
            case PHASE_UPDATED, MILESTONE_REACHED -> "MILESTONE";
            case SITE_REPORT_SUBMITTED -> "SITE_REPORT";
            case DOCUMENT_UPLOADED -> "DOCUMENT";
        };
    }

    private String getOrDefault(Map<String, String> meta, String key, String def) {
        if (meta == null) return def;
        return meta.getOrDefault(key, def);
    }
}
