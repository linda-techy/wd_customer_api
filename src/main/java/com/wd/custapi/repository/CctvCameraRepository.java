package com.wd.custapi.repository;

import com.wd.custapi.model.CctvCamera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CctvCameraRepository extends JpaRepository<CctvCamera, Long> {
    
    List<CctvCamera> findByProjectIdAndIsActiveTrue(Long projectId);
    
    List<CctvCamera> findByProjectIdAndIsInstalledTrue(Long projectId);
    
    List<CctvCamera> findByProjectId(Long projectId);
}

