package com.wd.custapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SQLDelete(sql = "UPDATE site_reports SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "site_reports")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SiteReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 50)
    private String status;
    
    @Column(name = "report_type", length = 50)
    private String reportType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_visit_id")
    private SiteVisit siteVisit;
    
    // For portal user who submitted the report (from wd_portal)
    @Column(name = "submitted_by")
    private Long submittedById;
    
    @Column(name = "submitted_by_name", length = 150)
    private String submittedByName;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "siteReport", cascade = CascadeType.ALL)
    private List<SiteReportPhoto> photos = new ArrayList<>();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Read-only mirrors of Portal-owned operational columns ──
    // The Portal API populates these via its own SiteReport entity
    // (which extends BaseEntity + has GPS/weather/manpower fields). The
    // Customer API doesn't write here; insertable/updatable=false keeps
    // these out of any Hibernate-generated INSERT/UPDATE statements.
    @Setter(AccessLevel.NONE)
    @Column(name = "weather", length = 100, insertable = false, updatable = false)
    private String weather;

    @Setter(AccessLevel.NONE)
    @Column(name = "manpower_deployed", insertable = false, updatable = false)
    private Integer manpowerDeployed;

    @Setter(AccessLevel.NONE)
    @Column(name = "equipment_used", columnDefinition = "TEXT", insertable = false, updatable = false)
    private String equipmentUsed;

    @Setter(AccessLevel.NONE)
    @Column(name = "work_progress", columnDefinition = "TEXT", insertable = false, updatable = false)
    private String workProgress;

    @Setter(AccessLevel.NONE)
    @Column(name = "latitude", insertable = false, updatable = false)
    private Double latitude;

    @Setter(AccessLevel.NONE)
    @Column(name = "longitude", insertable = false, updatable = false)
    private Double longitude;

    @Setter(AccessLevel.NONE)
    @Column(name = "distance_from_project", insertable = false, updatable = false)
    private Double distanceFromProject;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
        if (reportDate == null) reportDate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public SiteReport() {
        // Default constructor required by JPA
    }
}
