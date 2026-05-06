package com.wd.custapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only view of the {@code project_baseline} table owned by portal-api
 * (created by V119 in {@code wd_portal_api}).
 *
 * <p>Customer-API only needs {@code project_finish_date} for the expected
 * handover endpoint. {@code approved_by} is intentionally NOT mapped here
 * because the customer-API has no {@code portal_users} JPA entity to anchor
 * the FK against; leaving it unmapped lets Hibernate's create-drop schema in
 * tests work without dragging in the portal-side user model.
 */
@Immutable
@Entity
@Table(name = "project_baseline")
public class ProjectBaseline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false, unique = true)
    private Long projectId;

    @Column(name = "approved_at", nullable = false)
    private LocalDateTime approvedAt;

    @Column(name = "project_start_date", nullable = false)
    private LocalDate projectStartDate;

    @Column(name = "project_finish_date", nullable = false)
    private LocalDate projectFinishDate;

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDate getProjectStartDate() { return projectStartDate; }
    public LocalDate getProjectFinishDate() { return projectFinishDate; }
}
