package com.wd.custapi.repository;

import com.wd.custapi.model.FeedbackResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackResponseRepository extends JpaRepository<FeedbackResponse, Long> {
    
    List<FeedbackResponse> findByFormId(Long formId);
    
    List<FeedbackResponse> findByProjectIdAndCustomerId(Long projectId, Long customerId);
    
    Optional<FeedbackResponse> findByFormIdAndCustomerId(Long formId, Long customerId);
    
    List<FeedbackResponse> findByProjectIdOrderBySubmittedAtDesc(Long projectId);
}

