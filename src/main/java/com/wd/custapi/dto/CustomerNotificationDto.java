package com.wd.custapi.dto;

import com.wd.custapi.model.CustomerNotification;

import java.time.LocalDateTime;

/**
 * Customer-safe projection of CustomerNotification.
 * Intentionally omits the lazy customerUser association so raw entity columns
 * (including credentials on CustomerUser) cannot leak via Jackson serialization.
 */
public record CustomerNotificationDto(
        Long id,
        Long projectId,
        String title,
        String body,
        String notificationType,
        Long referenceId,
        boolean read,
        LocalDateTime createdAt
) {
    public static CustomerNotificationDto from(CustomerNotification n) {
        return new CustomerNotificationDto(
                n.getId(),
                n.getProjectId(),
                n.getTitle(),
                n.getBody(),
                n.getNotificationType(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
