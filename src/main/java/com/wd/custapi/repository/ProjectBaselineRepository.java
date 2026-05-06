package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectBaseline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProjectBaselineRepository extends JpaRepository<ProjectBaseline, Long> {

    /** Single-baseline-per-project semantics — at most one row per project_id. */
    Optional<ProjectBaseline> findByProjectId(Long projectId);
}
