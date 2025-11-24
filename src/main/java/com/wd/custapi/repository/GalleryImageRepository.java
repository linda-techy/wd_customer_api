package com.wd.custapi.repository;

import com.wd.custapi.model.GalleryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface GalleryImageRepository extends JpaRepository<GalleryImage, Long> {
    
    List<GalleryImage> findByProjectIdOrderByTakenDateDesc(Long projectId);
    
    List<GalleryImage> findByProjectIdAndTakenDate(Long projectId, LocalDate takenDate);
    
    List<GalleryImage> findBySiteReportId(Long siteReportId);
    
    List<GalleryImage> findByProjectIdAndTakenDateBetweenOrderByTakenDateDesc(
        Long projectId, LocalDate startDate, LocalDate endDate);
}

