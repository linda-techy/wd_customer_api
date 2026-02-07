package com.wd.custapi.repository;

import com.wd.custapi.model.SiteVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long> {

    List<SiteVisit> findByProjectIdOrderByCheckInTimeDesc(Long projectId);

    List<SiteVisit> findByProjectIdAndVisitorId(Long projectId, Long visitorId);

    Optional<SiteVisit> findTopByProjectIdAndVisitorIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(
            Long projectId, Long visitorId);

    @Query("SELECT s FROM SiteVisit s WHERE s.project.id = :projectId AND s.checkOutTime IS NOT NULL ORDER BY s.checkInTime DESC")
    List<SiteVisit> findByProjectIdAndCheckOutTimeIsNotNullOrderByCheckInTimeDesc(@Param("projectId") Long projectId);

    @Query("SELECT s FROM SiteVisit s WHERE s.project.id = :projectId AND s.checkOutTime IS NULL ORDER BY s.checkInTime DESC")
    List<SiteVisit> findByProjectIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(@Param("projectId") Long projectId);
}
