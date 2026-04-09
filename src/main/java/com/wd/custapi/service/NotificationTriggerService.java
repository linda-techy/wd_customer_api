package com.wd.custapi.service;

import com.wd.custapi.model.CustomerNotification;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.repository.CustomerNotificationRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.PaymentScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Event-driven notification triggers for key customer milestones.
 *
 * <ul>
 *   <li>Payment due 3-day reminder (scheduled daily job)</li>
 *   <li>Query replied (called by ProjectQueryService)</li>
 *   <li>Observation resolved (called by ObservationService)</li>
 * </ul>
 *
 * Each trigger writes an in-app notification to {@code customer_notifications}
 * and fires a Firebase FCM push (fire-and-forget — silently skipped if no FCM token).
 * Failures in this service must NEVER propagate to the calling business operation.
 */
@Service
public class NotificationTriggerService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationTriggerService.class);

    @Autowired
    private CustomerNotificationRepository notificationRepository;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private PaymentScheduleRepository paymentScheduleRepository;

    @Autowired
    private PushNotificationService pushNotificationService;

    // ─── Scheduled: Payment Due Reminders ────────────────────────────────────────

    /**
     * Daily job at 08:00 IST — sends payment-due reminders for schedules
     * where due_date = today + 3 days and status is still PENDING or UPCOMING.
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void sendPaymentDueReminders() {
        LocalDate target = LocalDate.now().plusDays(3);
        logger.info("NotificationTriggerService: scanning payment schedules due on {}", target);
        try {
            List<Object[]> upcoming = paymentScheduleRepository.findDueOn(target);
            for (Object[] row : upcoming) {
                try {
                    Long customerId = ((Number) row[0]).longValue();
                    Long scheduleId = ((Number) row[1]).longValue();
                    String description = (String) row[2];
                    java.math.BigDecimal amount = (java.math.BigDecimal) row[3];
                    Long projectId = row[4] != null ? ((Number) row[4]).longValue() : null;

                    customerUserRepository.findById(customerId).ifPresent(user -> {
                        String title = "Payment Due in 3 Days";
                        String body = String.format("₹%.0f due for: %s", amount, description);
                        saveAndPush(user, projectId, scheduleId, "PAYMENT_DUE", title, body);
                    });
                } catch (Exception e) {
                    logger.warn("Failed to send payment-due reminder for row: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("PaymentDueReminder job failed: {}", e.getMessage(), e);
        }
    }

    // ─── Triggered: Query Replied ─────────────────────────────────────────────────

    /**
     * Call this from {@code ProjectQueryService} when an engineer replies to a query.
     * Never throws — catches all exceptions internally.
     */
    public void notifyQueryReplied(Long customerId, Long projectId, Long queryId, String queryTitle) {
        try {
            customerUserRepository.findById(customerId).ifPresent(user -> {
                String title = "Query Replied";
                String body = "Your query has been answered: " + queryTitle;
                saveAndPush(user, projectId, queryId, "QUERY_REPLIED", title, body);
            });
        } catch (Exception e) {
            logger.warn("notifyQueryReplied failed (non-critical): {}", e.getMessage());
        }
    }

    // ─── Triggered: Observation Resolved ─────────────────────────────────────────

    /**
     * Call this from {@code ObservationService} when an observation is resolved.
     * Never throws — catches all exceptions internally.
     */
    public void notifyObservationResolved(Long customerId, Long projectId, Long observationId,
            String observationTitle) {
        try {
            customerUserRepository.findById(customerId).ifPresent(user -> {
                String title = "Issue Resolved";
                String body = "Your reported issue has been resolved: " + observationTitle;
                saveAndPush(user, projectId, observationId, "OBSERVATION_RESOLVED", title, body);
            });
        } catch (Exception e) {
            logger.warn("notifyObservationResolved failed (non-critical): {}", e.getMessage());
        }
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────────

    @Transactional
    private void saveAndPush(CustomerUser user, Long projectId, Long referenceId,
            String notificationType, String title, String body) {
        // 1. Persist in-app notification
        CustomerNotification notification = new CustomerNotification();
        notification.setCustomerUser(user);
        notification.setProjectId(projectId);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setNotificationType(notificationType);
        notification.setReferenceId(referenceId);
        notificationRepository.save(notification);

        // 2. Fire-and-forget FCM push (skip silently if no token)
        if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
            pushNotificationService.sendToToken(user.getFcmToken(), title, body,
                    java.util.Map.of("notificationType", notificationType));
        }

        logger.debug("Notification [{}] sent to user {} (project {})", notificationType,
                user.getId(), projectId);
    }
}
