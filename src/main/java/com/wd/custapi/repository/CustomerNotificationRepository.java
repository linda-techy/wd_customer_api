package com.wd.custapi.repository;

import com.wd.custapi.model.CustomerNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {

    /** Paginated list of notifications for a user, newest first. */
    Page<CustomerNotification> findByCustomerUser_IdOrderByCreatedAtDesc(Long customerUserId, Pageable pageable);

    /** Count of unread notifications for a user (used for badge). */
    long countByCustomerUser_IdAndReadFalse(Long customerUserId);

    /** Mark all notifications for a user as read (bulk update). */
    @Modifying
    @Query("UPDATE CustomerNotification n SET n.read = true WHERE n.customerUser.id = :userId AND n.read = false")
    int markAllReadByUserId(@Param("userId") Long userId);
}
