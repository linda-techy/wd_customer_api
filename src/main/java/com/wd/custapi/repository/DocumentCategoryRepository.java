package com.wd.custapi.repository;

import com.wd.custapi.model.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {
    
    Optional<DocumentCategory> findByName(String name);
    
    List<DocumentCategory> findAllByOrderByDisplayOrderAsc();
}

