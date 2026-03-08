package com.wd.custapi.repository;

import com.wd.custapi.model.PaymentSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

