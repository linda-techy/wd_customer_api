package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only entity mapping the project_phases table written by the portal API.
 * The customer API never writes to this table — it only reads phase data to
 * surface the construction timeline to customers.
 */
@Entity
@Table(name = "project_phases")
public class ProjectPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "phase_name", nullable = false, length = 100)
    private String phaseName;

    @Column(name = "planned_start")
    private LocalDate plannedStart;

    @Column(name = "planned_end")
    private LocalDate plannedEnd;

    @Column(name = "actual_start")
    private LocalDate actualStart;

    @Column(name = "actual_end")
    private LocalDate actualEnd;

    /** NOT_STARTED | IN_PROGRESS | COMPLETED | DELAYED */
    @Column(length = 20)
    private String status = "NOT_STARTED";

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ProjectPhase() {}

    // Getters — no setters needed (read-only from customer API perspective)

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getPhaseName() { return phaseName; }
    public LocalDate getPlannedStart() { return plannedStart; }
    public LocalDate getPlannedEnd() { return plannedEnd; }
    public LocalDate getActualStart() { return actualStart; }
    public LocalDate getActualEnd() { return actualEnd; }
    public String getStatus() { return status; }
    public Integer getDisplayOrder() { return displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
