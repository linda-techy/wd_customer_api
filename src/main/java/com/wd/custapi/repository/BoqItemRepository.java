package com.wd.custapi.repository;

import com.wd.custapi.model.BoqItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BoqItemRepository extends JpaRepository<BoqItem, Long> {
    
    List<BoqItem> findByProjectIdAndIsActiveTrue(Long projectId);
    
    List<BoqItem> findByProjectIdAndWorkTypeIdAndIsActiveTrue(Long projectId, Long workTypeId);

    @Query("SELECT b FROM BoqItem b JOIN FETCH b.project LEFT JOIN FETCH b.workType LEFT JOIN FETCH b.category WHERE b.project.id = :projectId AND b.isActive = true AND b.deletedAt IS NULL")
    List<BoqItem> findByProjectIdWithAssociations(@Param("projectId") Long projectId);
    
    List<BoqItem> findByProjectIdOrderByWorkTypeIdAsc(Long projectId);
    
    @Query("SELECT SUM(b.quantity * b.rate) FROM BoqItem b WHERE b.project.id = :projectId AND b.isActive = true AND b.deletedAt IS NULL")
    BigDecimal getTotalAmountByProjectId(Long projectId);
}

