package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectPhaseRepository extends JpaRepository<ProjectPhase, Long> {

    /**
     * Returns all phases for a project ordered by display_order ASC.
     * Customers see the construction timeline in the correct sequence.
     */
    @Query("SELECT p FROM ProjectPhase p WHERE p.project.id = :projectId ORDER BY p.displayOrder ASC")
    List<ProjectPhase> findByProjectIdOrderByDisplayOrder(@Param("projectId") Long projectId);
}
