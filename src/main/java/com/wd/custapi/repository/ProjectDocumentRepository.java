package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {
    
    List<ProjectDocument> findByProjectIdAndIsActiveTrue(Long projectId);
    
    List<ProjectDocument> findByProjectIdAndCategoryIdAndIsActiveTrue(Long projectId, Long categoryId);
    
    List<ProjectDocument> findByProjectIdAndCategoryId(Long projectId, Long categoryId);
    
    List<ProjectDocument> findByProjectIdOrderByUploadDateDesc(Long projectId);
    
    // Include documents where isActive is true or NULL (NULL treated as active)
    @Query("SELECT d FROM ProjectDocument d WHERE d.project.id = :projectId AND (d.isActive = true OR d.isActive IS NULL) ORDER BY d.uploadDate DESC")
    List<ProjectDocument> findByProjectIdActiveOrNull(@Param("projectId") Long projectId);
    
    // Include documents where isActive is true or NULL with category filter
    @Query("SELECT d FROM ProjectDocument d WHERE d.project.id = :projectId AND d.category.id = :categoryId AND (d.isActive = true OR d.isActive IS NULL) ORDER BY d.uploadDate DESC")
    List<ProjectDocument> findByProjectIdAndCategoryIdActiveOrNull(@Param("projectId") Long projectId, @Param("categoryId") Long categoryId);
    
    // Get ALL documents for a project regardless of isActive status (for debugging/fixing issues)
    @Query("SELECT d FROM ProjectDocument d WHERE d.project.id = :projectId ORDER BY d.uploadDate DESC")
    List<ProjectDocument> findAllByProjectId(@Param("projectId") Long projectId);
    
    // Get ALL documents for a project and category regardless of isActive status
    @Query("SELECT d FROM ProjectDocument d WHERE d.project.id = :projectId AND d.category.id = :categoryId ORDER BY d.uploadDate DESC")
    List<ProjectDocument> findAllByProjectIdAndCategoryId(@Param("projectId") Long projectId, @Param("categoryId") Long categoryId);
}

