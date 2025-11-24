package com.wd.custapi.repository;

import com.wd.custapi.model.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {
    
    List<QualityCheck> findByProjectIdAndStatus(Long projectId, QualityCheck.QualityCheckStatus status);
    
    List<QualityCheck> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    
    List<QualityCheck> findByProjectIdAndStatusOrderByPriorityDescCreatedAtDesc(
        Long projectId, QualityCheck.QualityCheckStatus status);
}

