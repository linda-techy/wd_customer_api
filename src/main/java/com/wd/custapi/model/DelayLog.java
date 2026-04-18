package com.wd.custapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Immutable
@Entity
@Table(name = "delay_logs")
public class DelayLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "delay_type", nullable = false, length = 50)
    private String delayType;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "reason_text", columnDefinition = "TEXT")
    private String reasonText;

    @Column(name = "reason_category", length = 50)
    private String reasonCategory;

    @Column(name = "responsible_party")
    private String responsibleParty;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "impact_description", columnDefinition = "TEXT")
    private String impactDescription;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters only — write operations are Portal API's responsibility
    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public String getDelayType() { return delayType; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public String getReasonText() { return reasonText; }
    public String getReasonCategory() { return reasonCategory; }
    public String getResponsibleParty() { return responsibleParty; }
    public Integer getDurationDays() { return durationDays; }
    public String getImpactDescription() { return impactDescription; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
