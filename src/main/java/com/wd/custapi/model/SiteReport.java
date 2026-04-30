package com.wd.custapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @Column(name = "weather", length = 100, insertable = false, updatable = false)
    private String weather;

    @Column(name = "manpower_deployed", insertable = false, updatable = false)
    private Integer manpowerDeployed;

    @Column(name = "equipment_used", columnDefinition = "TEXT", insertable = false, updatable = false)
    private String equipmentUsed;

    @Column(name = "work_progress", columnDefinition = "TEXT", insertable = false, updatable = false)
    private String workProgress;

    @Column(name = "latitude", insertable = false, updatable = false)
    private Double latitude;

    @Column(name = "longitude", insertable = false, updatable = false)
    private Double longitude;

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
    public SiteReport() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Project getProject() {
        return project;
    }
    
    public void setProject(Project project) {
        this.project = project;
    }
    
    public LocalDateTime getReportDate() {
        return reportDate;
    }
    
    public void setReportDate(LocalDateTime reportDate) {
        this.reportDate = reportDate;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
    
    public SiteVisit getSiteVisit() {
        return siteVisit;
    }
    
    public void setSiteVisit(SiteVisit siteVisit) {
        this.siteVisit = siteVisit;
    }
    
    public Long getSubmittedById() {
        return submittedById;
    }
    
    public void setSubmittedById(Long submittedById) {
        this.submittedById = submittedById;
    }
    
    public String getSubmittedByName() {
        return submittedByName;
    }
    
    public void setSubmittedByName(String submittedByName) {
        this.submittedByName = submittedByName;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public List<SiteReportPhoto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<SiteReportPhoto> photos) {
        this.photos = photos;
    }

    public String getWeather() { return weather; }
    public Integer getManpowerDeployed() { return manpowerDeployed; }
    public String getEquipmentUsed() { return equipmentUsed; }
    public String getWorkProgress() { return workProgress; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getDistanceFromProject() { return distanceFromProject; }
}
