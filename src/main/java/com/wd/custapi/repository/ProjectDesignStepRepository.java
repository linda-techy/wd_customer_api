package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectDesignStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDesignStepRepository extends JpaRepository<ProjectDesignStep, Long> {
    List<ProjectDesignStep> findByProjectId(Long projectId);

    @Query("SELECT SUM(ds.weightPercentage * pds.progressPercentage / 100) FROM ProjectDesignStep pds JOIN pds.step ds WHERE pds.project.id = :projectId")
    Double calculateDesignProgress(@Param("projectId") Long projectId);
}
