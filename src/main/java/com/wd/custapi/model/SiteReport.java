package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "site_reports")
public class SiteReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 100)
    private String weather;
    
    @Column(name = "work_progress", columnDefinition = "TEXT")
    private String workProgress;
    
    @Column(name = "manpower_deployed")
    private Integer manpowerDeployed;
    
    @Column(name = "equipment_used", columnDefinition = "TEXT")
    private String equipmentUsed;
    
    @Column(length = 50)
    private String status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private CustomerUser createdBy;
    
    // For portal user who submitted the report (from wd_portal)
    @Column(name = "submitted_by")
    private Long submittedById;
    
    // Transient field to hold portal user name for display
    @Transient
    private String submittedByName;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
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
    
    public LocalDate getReportDate() {
        return reportDate;
    }
    
    public void setReportDate(LocalDate reportDate) {
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
    
    public String getWeather() {
        return weather;
    }
    
    public void setWeather(String weather) {
        this.weather = weather;
    }
    
    public String getWorkProgress() {
        return workProgress;
    }
    
    public void setWorkProgress(String workProgress) {
        this.workProgress = workProgress;
    }
    
    public Integer getManpowerDeployed() {
        return manpowerDeployed;
    }
    
    public void setManpowerDeployed(Integer manpowerDeployed) {
        this.manpowerDeployed = manpowerDeployed;
    }
    
    public String getEquipmentUsed() {
        return equipmentUsed;
    }
    
    public void setEquipmentUsed(String equipmentUsed) {
        this.equipmentUsed = equipmentUsed;
    }
    
    public CustomerUser getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(CustomerUser createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
}

