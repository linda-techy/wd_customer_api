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

    // Bulk fetch across all projects in one query — eliminates N+1 loop
    @org.springframework.data.jpa.repository.Query(
        "SELECT a FROM ActivityFeed a WHERE a.project.id IN :projectIds ORDER BY a.createdAt DESC")
    List<ActivityFeed> findTop10ByProjectIdInOrderByCreatedAtDesc(
        @org.springframework.data.repository.query.Param("projectIds") List<Long> projectIds,
        org.springframework.data.domain.Pageable pageable);
}


