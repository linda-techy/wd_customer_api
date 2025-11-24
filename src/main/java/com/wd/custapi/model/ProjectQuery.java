package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_queries")
public class ProjectQuery {
    
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
    @JoinColumn(name = "raised_by_id", nullable = false)
    private CustomerUser raisedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raised_by_role_id")
    private StaffRole raisedByRole;
    
    @Column(name = "raised_date", nullable = false)
    private LocalDateTime raisedDate = LocalDateTime.now();
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private QueryStatus status = QueryStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority priority = Priority.MEDIUM;
    
    @Column(length = 50)
    private String category;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private CustomerUser assignedTo;
    
    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private CustomerUser resolvedBy;
    
    @Column(columnDefinition = "TEXT")
    private String resolution;
    
    // Enums
    public enum QueryStatus {
        ACTIVE, RESOLVED
    }
    
    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
    
    // Constructors
    public ProjectQuery() {}
    
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
    
    public CustomerUser getRaisedBy() {
        return raisedBy;
    }
    
    public void setRaisedBy(CustomerUser raisedBy) {
        this.raisedBy = raisedBy;
    }
    
    public StaffRole getRaisedByRole() {
        return raisedByRole;
    }
    
    public void setRaisedByRole(StaffRole raisedByRole) {
        this.raisedByRole = raisedByRole;
    }
    
    public LocalDateTime getRaisedDate() {
        return raisedDate;
    }
    
    public void setRaisedDate(LocalDateTime raisedDate) {
        this.raisedDate = raisedDate;
    }
    
    public QueryStatus getStatus() {
        return status;
    }
    
    public void setStatus(QueryStatus status) {
        this.status = status;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public CustomerUser getAssignedTo() {
        return assignedTo;
    }
    
    public void setAssignedTo(CustomerUser assignedTo) {
        this.assignedTo = assignedTo;
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
    
    public String getResolution() {
        return resolution;
    }
    
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
}

