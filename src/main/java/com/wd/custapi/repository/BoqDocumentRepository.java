package com.wd.custapi.repository;

import com.wd.custapi.model.BoqDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoqDocumentRepository extends JpaRepository<BoqDocument, Long> {

    Optional<BoqDocument> findByProjectIdAndStatus(Long projectId, String status);

    Optional<BoqDocument> findTopByProjectIdOrderByRevisionNumberDesc(Long projectId);

    Optional<BoqDocument> findTopByProjectIdAndStatusNotOrderByRevisionNumberDesc(Long projectId, String status);

    List<BoqDocument> findByProjectIdOrderByRevisionNumberAsc(Long projectId);

    @Modifying
    @Transactional
    @Query("UPDATE BoqDocument d SET d.customerAcknowledgedAt = :at, d.customerAcknowledgedBy = :customerId WHERE d.id = :id")
    void recordAcknowledgement(@Param("id") Long id, @Param("at") LocalDateTime at, @Param("customerId") Long customerId);
}
