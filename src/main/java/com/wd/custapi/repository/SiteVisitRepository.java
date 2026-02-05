package com.wd.custapi.repository;

import com.wd.custapi.model.SiteVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long> {
    
    List<SiteVisit> findByProjectIdOrderByCheckInTimeDesc(Long projectId);
    
    List<SiteVisit> findByProjectIdAndVisitorId(Long projectId, Long visitorId);
    
    Optional<SiteVisit> findTopByProjectIdAndVisitorIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(
        Long projectId, Long visitorId);
    
    List<SiteVisit> findByProjectIdAndCheckOutTimeIsNotNullOrderByCheckInTimeDesc(Long projectId);
    
    List<SiteVisit> findByProjectIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long projectId);
}

