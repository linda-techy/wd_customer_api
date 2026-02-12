package com.wd.custapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    
    // Transient field to hold portal user name for display
    @Transient
    private String submittedByName;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "siteReport", cascade = CascadeType.ALL)
    private List<SiteReportPhoto> photos = new ArrayList<>();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
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
}
