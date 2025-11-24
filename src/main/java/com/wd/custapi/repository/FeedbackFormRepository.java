package com.wd.custapi.repository;

import com.wd.custapi.model.FeedbackForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackFormRepository extends JpaRepository<FeedbackForm, Long> {
    
    List<FeedbackForm> findByProjectIdAndIsActiveTrue(Long projectId);
    
    List<FeedbackForm> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}

