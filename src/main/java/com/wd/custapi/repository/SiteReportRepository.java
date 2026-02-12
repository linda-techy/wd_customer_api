package com.wd.custapi.repository;

import com.wd.custapi.model.SiteReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SiteReportRepository extends JpaRepository<SiteReport, Long> {
    
    List<SiteReport> findByProjectIdOrderByReportDateDesc(Long projectId);
    
    List<SiteReport> findByProjectIdAndReportDateBetweenOrderByReportDateDesc(
        Long projectId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find site reports for multiple projects with pagination.
     * Used by customer portal to fetch reports for all customer's projects.
     */
    Page<SiteReport> findByProjectIdIn(List<Long> projectIds, Pageable pageable);
}
