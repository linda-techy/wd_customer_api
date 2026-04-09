package com.wd.custapi.controller;

import com.wd.custapi.dto.ProjectModuleDtos.ApiResponse;
import com.wd.custapi.model.CustomerNotification;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.repository.CustomerNotificationRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * In-app notification endpoints for the customer app.
 * Notifications are created by portal-side actions (site reports, payments, BOQ approvals, etc.)
 * and consumed here by the customer app notification bell and screen.
 */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'ARCHITECT', 'INTERIOR_DESIGNER', 'SITE_ENGINEER', 'VIEWER', 'CUSTOMER_ADMIN', 'CONTRACTOR', 'BUILDER')")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    private final CustomerNotificationRepository notificationRepository;
    private final CustomerUserRepository customerUserRepository;

    public NotificationController(
            CustomerNotificationRepository notificationRepository,
            CustomerUserRepository customerUserRepository) {
        this.notificationRepository = notificationRepository;
        this.customerUserRepository = customerUserRepository;
    }

    /**
     * GET /api/notifications?page=0&size=20
     * Returns paginated notifications for the authenticated user, newest first.
     * Response includes unread count for badge display.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            CustomerUser user = resolveUser(auth.getName());
            Pageable pageable = PageRequest.of(page, Math.min(size, 50));
            Page<CustomerNotification> notifications =
                    notificationRepository.findByCustomerUser_IdOrderByCreatedAtDesc(user.getId(), pageable);
            long unreadCount = notificationRepository.countByCustomerUser_IdAndReadFalse(user.getId());

            Map<String, Object> response = Map.of(
                    "notifications", notifications.getContent(),
                    "totalElements", notifications.getTotalElements(),
                    "totalPages", notifications.getTotalPages(),
                    "currentPage", page,
                    "unreadCount", unreadCount
            );
            return ResponseEntity.ok(new ApiResponse<>(true, "Notifications retrieved", response));
        } catch (Exception e) {
            logger.error("Error fetching notifications for {}: {}", auth.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve notifications", null));
        }
    }

    /**
     * PUT /api/notifications/{id}/read
     * Mark a single notification as read.
     */
    @PutMapping("/{id}/read")
    @Transactional
    public ResponseEntity<ApiResponse<String>> markRead(
            @PathVariable Long id,
            Authentication auth) {
        try {
            CustomerUser user = resolveUser(auth.getName());
            CustomerNotification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));

            if (!notification.getCustomerUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Access denied", null));
            }
            notification.setRead(true);
            notificationRepository.save(notification);
            return ResponseEntity.ok(new ApiResponse<>(true, "Notification marked as read", "ok"));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, e.getMessage(), null));
            }
            logger.error("Error marking notification {} as read: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to mark notification as read", null));
        }
    }

    /**
     * PUT /api/notifications/read-all
     * Mark all notifications for the current user as read.
     */
    @PutMapping("/read-all")
    @Transactional
    public ResponseEntity<ApiResponse<String>> markAllRead(Authentication auth) {
        try {
            CustomerUser user = resolveUser(auth.getName());
            int updated = notificationRepository.markAllReadByUserId(user.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, updated + " notification(s) marked as read", "ok"));
        } catch (Exception e) {
            logger.error("Error marking all notifications as read for {}: {}", auth.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to mark notifications as read", null));
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private CustomerUser resolveUser(String email) {
        return customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer user not found for email: " + email));
    }
}
