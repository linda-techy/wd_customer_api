package com.wd.custapi.repository;

import com.wd.custapi.model.PaymentSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {

    /**
     * Find payment schedules for projects via design_package_payments relationship.
     * Used by customer portal to fetch payments for all customer's projects.
     */
    @Query("SELECT ps FROM PaymentSchedule ps WHERE ps.designPayment.project.id IN :projectIds")
    Page<PaymentSchedule> findByProjectIdIn(@Param("projectIds") List<Long> projectIds, Pageable pageable);

    /**
     * Aggregate query that avoids loading all rows into memory.
     * Returns counts and sums directly from the database.
     */
    /**
     * Returns payment schedules due on a specific date with status PENDING or UPCOMING.
     * Result columns: [customerUserId, scheduleId, description, amount, projectId]
     * Used by NotificationTriggerService to send 3-day payment-due reminders.
     */
    @Query(value =
            "SELECT pm.customer_user_id, ps.id, ps.description, ps.amount, p.id AS project_id " +
            "FROM payment_schedule ps " +
            "JOIN design_package_payments dpp ON ps.design_payment_id = dpp.id " +
            "JOIN customer_projects p ON dpp.project_id = p.id " +
            "JOIN project_members pm ON pm.project_id = p.id " +
            "WHERE ps.due_date = :dueDate " +
            "AND UPPER(ps.status) IN ('PENDING','UPCOMING') " +
            "AND p.deleted_at IS NULL", nativeQuery = true)
    List<Object[]> findDueOn(@Param("dueDate") LocalDate dueDate);

    @Query("""
        SELECT
            COUNT(ps)                                                          AS totalBills,
            SUM(CASE WHEN UPPER(ps.status) IN ('PENDING','OVERDUE') THEN 1 ELSE 0 END) AS pendingBills,
            SUM(CASE WHEN UPPER(ps.status) = 'PAID'                 THEN 1 ELSE 0 END) AS paidBills,
            COALESCE(SUM(ps.amount), 0)                                        AS totalAmount,
            COALESCE(SUM(CASE WHEN UPPER(ps.status) IN ('PENDING','OVERDUE') THEN ps.amount ELSE 0 END), 0) AS pendingAmount
        FROM PaymentSchedule ps
        WHERE ps.designPayment.project.id IN :projectIds
        """)
    Object[] getPaymentSummaryForProjects(@Param("projectIds") List<Long> projectIds);
}

