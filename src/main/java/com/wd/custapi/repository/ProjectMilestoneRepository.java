package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Read-only access to project_milestones table (data written by portal API).
 * Used to populate ScheduleScreen in the customer app with real milestone data.
 */
public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {

    /** Returns milestones for a project ordered by due date ascending. */
    List<ProjectMilestone> findByProjectIdOrderByDueDateAsc(Long projectId);

    /** Returns milestones for a project ordered by insertion order (id).
     *  Used by the Timeline endpoint as the canonical milestone order. */
    List<ProjectMilestone> findByProjectIdOrderByIdAsc(Long projectId);
}
