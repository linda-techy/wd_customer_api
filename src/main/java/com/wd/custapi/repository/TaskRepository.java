package com.wd.custapi.repository;

import com.wd.custapi.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectIdOrderByDueDateAsc(Long projectId);

    List<Task> findByProjectIdAndStatusOrderByDueDateAsc(Long projectId, String status);

    /** Gantt query: all tasks for a project ordered by start_date (nulls last). */
    @org.springframework.data.jpa.repository.Query(
            "SELECT t FROM Task t WHERE t.project.id = :projectId " +
            "ORDER BY CASE WHEN t.startDate IS NULL THEN 1 ELSE 0 END, t.startDate ASC")
    List<Task> findByProjectIdOrderedForGantt(
            @org.springframework.data.repository.query.Param("projectId") Long projectId);
}
