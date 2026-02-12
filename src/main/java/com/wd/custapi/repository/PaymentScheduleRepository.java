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
}
