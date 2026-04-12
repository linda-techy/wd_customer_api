package com.wd.custapi.repository;

import com.wd.custapi.model.BoqDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BoqDocumentRepository extends JpaRepository<BoqDocument, Long> {

    Optional<BoqDocument> findByProjectIdAndStatus(Long projectId, String status);

    Optional<BoqDocument> findTopByProjectIdOrderByRevisionNumberDesc(Long projectId);
}
