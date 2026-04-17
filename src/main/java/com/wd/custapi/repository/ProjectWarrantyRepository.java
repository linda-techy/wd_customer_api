package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectWarranty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectWarrantyRepository extends JpaRepository<ProjectWarranty, Long> {
    List<ProjectWarranty> findByProjectIdOrderByEndDateDesc(Long projectId);
}
