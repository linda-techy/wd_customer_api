package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "observations")
public class Observation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_id", nullable = false)
    private CustomerUser reportedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_role_id")
    private StaffRole reportedByRole;
    
    @Column(name = "reported_date", nullable = false)
    private LocalDateTime reportedDate = LocalDateTime.now();
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ObservationStatus status = ObservationStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority priority = Priority.MEDIUM;
    
    @Column(length = 255)
    private String location;
    
    @Column(name = "image_path", length = 500)
    private String imagePath;
    
    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private CustomerUser resolvedBy;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    // Enums
    public enum ObservationStatus {
        ACTIVE, RESOLVED
    }
    
    public enum Priority {
        LOW, MEDIUM, HIGH
    }
    
    // Constructors
    public Observation() {}
    
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
    
    public CustomerUser getReportedBy() {
        return reportedBy;
    }
    
    public void setReportedBy(CustomerUser reportedBy) {
        this.reportedBy = reportedBy;
    }
    
    public StaffRole getReportedByRole() {
        return reportedByRole;
    }
    
    public void setReportedByRole(StaffRole reportedByRole) {
        this.reportedByRole = reportedByRole;
    }
    
    public LocalDateTime getReportedDate() {
        return reportedDate;
    }
    
    public void setReportedDate(LocalDateTime reportedDate) {
        this.reportedDate = reportedDate;
    }
    
    public ObservationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ObservationStatus status) {
        this.status = status;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public LocalDateTime getResolvedDate() {
        return resolvedDate;
    }
    
    public void setResolvedDate(LocalDateTime resolvedDate) {
        this.resolvedDate = resolvedDate;
    }
    
    public CustomerUser getResolvedBy() {
        return resolvedBy;
    }
    
    public void setResolvedBy(CustomerUser resolvedBy) {
        this.resolvedBy = resolvedBy;
    }
    
    public String getResolutionNotes() {
        return resolutionNotes;
    }
    
    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}

