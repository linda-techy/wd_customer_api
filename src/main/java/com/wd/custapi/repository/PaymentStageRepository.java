package com.wd.custapi.repository;

import com.wd.custapi.model.PaymentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentStageRepository extends JpaRepository<PaymentStage, Long> {

    List<PaymentStage> findByProjectIdOrderByStageNumberAsc(Long projectId);

    List<PaymentStage> findByBoqDocumentIdOrderByStageNumberAsc(Long boqDocumentId);
}
