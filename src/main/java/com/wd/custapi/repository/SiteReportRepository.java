package com.wd.custapi.repository;

import com.wd.custapi.model.SiteReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Uses LEFT JOIN FETCH on photos to avoid N+1 queries when the DTO
     * accesses the photos collection for each report.
     */
    @Query("SELECT DISTINCT sr FROM SiteReport sr LEFT JOIN FETCH sr.photos " +
           "WHERE sr.project.id IN :projectIds")
    List<SiteReport> findByProjectIdInWithPhotos(@Param("projectIds") List<Long> projectIds);

    /**
     * Count query companion required for pagination when using JOIN FETCH.
     */
    @Query("SELECT COUNT(DISTINCT sr) FROM SiteReport sr WHERE sr.project.id IN :projectIds")
    long countByProjectIdIn(@Param("projectIds") List<Long> projectIds);

    /**
     * Paginated query without JOIN FETCH — photos are eagerly loaded via
     * a separate batch when the collection is first accessed (avoids
     * the Hibernate HHH90003004 "firstResult/maxResults specified with
     * collection fetch" warning).
     * Use {@link #findByProjectIdInWithPhotos} for non-paginated bulk fetches.
     */
    @EntityGraph(attributePaths = {"photos"})
    Page<SiteReport> findByProjectIdIn(List<Long> projectIds, Pageable pageable);
}
