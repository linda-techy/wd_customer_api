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

    /**
     * Returns all active BOQ items for the given project regardless of document status.
     * Used internally (e.g. summary calculations, admin views).
     */
    @Query("SELECT b FROM BoqItem b JOIN FETCH b.project LEFT JOIN FETCH b.workType LEFT JOIN FETCH b.category WHERE b.project.id = :projectId AND b.isActive = true AND b.deletedAt IS NULL")
    List<BoqItem> findByProjectIdWithAssociations(@Param("projectId") Long projectId);

    /**
     * Visibility-gate query for customer-facing BOQ display.
     *
     * Rules:
     *  1. If an APPROVED boq_document exists for the project, only return items
     *     linked to that document (boq_document_id matches an APPROVED row).
     *  2. Items with a null boq_document_id are legacy rows (pre-V19) that were
     *     never linked — include them only when NO approved document exists yet,
     *     maintaining backward compatibility.
     *
     * In practice this means:
     *  - DRAFT / PENDING_APPROVAL items are hidden from the customer until
     *    Portal explicitly approves the document.
     *  - Once approved, only items in that approved document are shown.
     */
    @Query("""
        SELECT b FROM BoqItem b
        JOIN FETCH b.project
        LEFT JOIN FETCH b.workType
        LEFT JOIN FETCH b.category
        WHERE b.project.id = :projectId
          AND b.isActive = true
          AND b.deletedAt IS NULL
          AND (
            (b.boqDocumentId IS NOT NULL AND EXISTS (
                SELECT d.id FROM BoqDocument d
                WHERE d.id = b.boqDocumentId
                  AND d.status = 'APPROVED'
            ))
            OR
            (b.boqDocumentId IS NULL AND NOT EXISTS (
                SELECT d.id FROM BoqDocument d
                WHERE d.project.id = :projectId
                  AND d.status = 'APPROVED'
            ))
          )
        """)
    List<BoqItem> findApprovedByProjectId(@Param("projectId") Long projectId);
    
    List<BoqItem> findByProjectIdOrderByWorkTypeIdAsc(Long projectId);
    
    @Query("SELECT SUM(b.quantity * b.rate) FROM BoqItem b WHERE b.project.id = :projectId AND b.isActive = true AND b.deletedAt IS NULL")
    BigDecimal getTotalAmountByProjectId(Long projectId);
}

