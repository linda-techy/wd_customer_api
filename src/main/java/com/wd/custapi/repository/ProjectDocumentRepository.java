package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {

    String REFERENCE_TYPE_PROJECT = "PROJECT";

    List<ProjectDocument> findByReferenceIdAndReferenceTypeAndIsActiveTrue(Long referenceId, String referenceType);

    List<ProjectDocument> findByReferenceIdAndReferenceTypeAndCategoryIdAndIsActiveTrue(
            Long referenceId, String referenceType, Long categoryId);

    List<ProjectDocument> findByReferenceIdAndReferenceTypeOrderByCreatedAtDesc(Long referenceId, String referenceType);

    @Query("SELECT d FROM ProjectDocument d WHERE d.referenceId = :referenceId AND d.referenceType = :referenceType AND (d.isActive = true OR d.isActive IS NULL) ORDER BY d.createdAt DESC")
    List<ProjectDocument> findByReferenceIdAndReferenceTypeActiveOrNull(
            @Param("referenceId") Long referenceId,
            @Param("referenceType") String referenceType);

    @Query("SELECT d FROM ProjectDocument d WHERE d.referenceId = :referenceId AND d.referenceType = :referenceType AND d.category.id = :categoryId AND (d.isActive = true OR d.isActive IS NULL) ORDER BY d.createdAt DESC")
    List<ProjectDocument> findByReferenceIdAndReferenceTypeAndCategoryIdActiveOrNull(
            @Param("referenceId") Long referenceId,
            @Param("referenceType") String referenceType,
            @Param("categoryId") Long categoryId);

    @Query("SELECT d FROM ProjectDocument d WHERE d.referenceId = :referenceId AND d.referenceType = :referenceType ORDER BY d.createdAt DESC")
    List<ProjectDocument> findAllByReferenceIdAndReferenceType(
            @Param("referenceId") Long referenceId,
            @Param("referenceType") String referenceType);

    @Query("SELECT d FROM ProjectDocument d WHERE d.referenceId = :referenceId AND d.referenceType = :referenceType AND d.category.id = :categoryId ORDER BY d.createdAt DESC")
    List<ProjectDocument> findAllByReferenceIdAndReferenceTypeAndCategoryId(
            @Param("referenceId") Long referenceId,
            @Param("referenceType") String referenceType,
            @Param("categoryId") Long categoryId);
}
