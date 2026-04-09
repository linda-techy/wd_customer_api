package com.wd.custapi.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only view of the project_milestones table (written by portal API).
 * Used to populate the customer app's ScheduleScreen with real milestone data.
 */
@Entity
@Table(name = "project_milestones")
public class ProjectMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "status", length = 20)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, INVOICED

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "completion_percentage", precision = 5, scale = 2)
    private BigDecimal completionPercentage;

    @Column(name = "weight_percentage", precision = 5, scale = 2)
    private BigDecimal weightPercentage;

    @Column(name = "milestone_percentage", precision = 5, scale = 2)
    private BigDecimal milestonePercentage;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters
    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getCompletedDate() { return completedDate; }
    public BigDecimal getCompletionPercentage() { return completionPercentage; }
    public BigDecimal getWeightPercentage() { return weightPercentage; }
    public BigDecimal getMilestonePercentage() { return milestonePercentage; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getActualStartDate() { return actualStartDate; }
    public LocalDate getActualEndDate() { return actualEndDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
