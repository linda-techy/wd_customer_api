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

    /**
     * Returns the latest CPM-computed early-finish date across all tasks of a
     * project, or {@code Optional.empty()} when no row has an ef_date set yet
     * (i.e., CPM has not run for the project). Used by the customer-facing
     * expected-handover endpoint.
     */
    @org.springframework.data.jpa.repository.Query(
            "SELECT MAX(t.efDate) FROM Task t WHERE t.project.id = :projectId AND t.efDate IS NOT NULL")
    java.util.Optional<java.time.LocalDate> findMaxEfDateByProjectId(
            @org.springframework.data.repository.query.Param("projectId") Long projectId);
}
