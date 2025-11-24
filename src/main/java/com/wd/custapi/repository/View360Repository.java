package com.wd.custapi.repository;

import com.wd.custapi.model.View360;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface View360Repository extends JpaRepository<View360, Long> {
    
    List<View360> findByProjectIdAndIsActiveTrue(Long projectId);
    
    List<View360> findByProjectIdOrderByUploadedAtDesc(Long projectId);
}

