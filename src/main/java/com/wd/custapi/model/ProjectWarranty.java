package com.wd.custapi.model;

import com.wd.custapi.model.enums.WarrantyStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Immutable
@Entity
@Table(name = "project_warranties")
public class ProjectWarranty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "component_name", nullable = false)
    private String componentName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private WarrantyStatus status;

    @Column(name = "coverage_details", columnDefinition = "TEXT")
    private String coverageDetails;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getComponentName() { return componentName; }
    public String getDescription() { return description; }
    public String getProviderName() { return providerName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public WarrantyStatus getStatus() { return status; }
    public String getCoverageDetails() { return coverageDetails; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
