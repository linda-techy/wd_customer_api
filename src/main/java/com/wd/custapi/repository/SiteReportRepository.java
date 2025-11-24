package com.wd.custapi.repository;

import com.wd.custapi.model.SiteReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SiteReportRepository extends JpaRepository<SiteReport, Long> {
    
    List<SiteReport> findByProjectIdOrderByReportDateDesc(Long projectId);
    
    List<SiteReport> findByProjectIdAndReportDate(Long projectId, LocalDate reportDate);
    
    List<SiteReport> findByProjectIdAndReportDateBetweenOrderByReportDateDesc(
        Long projectId, LocalDate startDate, LocalDate endDate);
}

