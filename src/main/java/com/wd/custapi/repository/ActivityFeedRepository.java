package com.wd.custapi.repository;

import com.wd.custapi.model.ActivityFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityFeedRepository extends JpaRepository<ActivityFeed, Long> {
    
    List<ActivityFeed> findByProjectIdOrderByCreatedAtDesc(Long projectId);
    
    List<ActivityFeed> findByProjectIdAndCreatedAtBetweenOrderByCreatedAtDesc(
        Long projectId, LocalDateTime startDate, LocalDateTime endDate);
    
    List<ActivityFeed> findByProjectIdAndActivityTypeIdOrderByCreatedAtDesc(
        Long projectId, Long activityTypeId);
}

