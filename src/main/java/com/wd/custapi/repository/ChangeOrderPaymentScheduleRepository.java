package com.wd.custapi.repository;

import com.wd.custapi.model.ChangeOrderPaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChangeOrderPaymentScheduleRepository extends JpaRepository<ChangeOrderPaymentSchedule, Long> {

    Optional<ChangeOrderPaymentSchedule> findByChangeOrderId(Long changeOrderId);
}
