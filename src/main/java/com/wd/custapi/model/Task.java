package com.wd.custapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only view of tasks table for customer-facing API.
 * Portal API manages all mutations.
 */
@Immutable
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // ===== Gantt / Scheduling fields (V53) — read-only =====

    @Column(name = "start_date", insertable = false, updatable = false)
    private LocalDate startDate;

    @Column(name = "end_date", insertable = false, updatable = false)
    private LocalDate endDate;

    @Column(name = "depends_on_task_id", insertable = false, updatable = false)
    private Long dependsOnTaskId;

    @Column(name = "progress_percent", insertable = false, updatable = false)
    private Integer progressPercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters (read-only — no setters)
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public Long getDependsOnTaskId() { return dependsOnTaskId; }
    public Integer getProgressPercent() { return progressPercent; }
    public Project getProject() { return project; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
