package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "site_visits")
public class SiteVisit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id", nullable = false)
    private CustomerUser visitor;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_role_id")
    private StaffRole visitorRole;
    
    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;
    
    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;
    
    @Column(length = 255)
    private String purpose;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(columnDefinition = "TEXT")
    private String findings;
    
    @Column(length = 255)
    private String location;
    
    @Column(name = "weather_conditions", length = 100)
    private String weatherConditions;
    
    @Column(columnDefinition = "varchar(255)[]")
    private String[] attendees;
    
    // Constructors
    public SiteVisit() {}
    
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
    
    public CustomerUser getVisitor() {
        return visitor;
    }
    
    public void setVisitor(CustomerUser visitor) {
        this.visitor = visitor;
    }
    
    public StaffRole getVisitorRole() {
        return visitorRole;
    }
    
    public void setVisitorRole(StaffRole visitorRole) {
        this.visitorRole = visitorRole;
    }
    
    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }
    
    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }
    
    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }
    
    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public String getFindings() {
        return findings;
    }
    
    public void setFindings(String findings) {
        this.findings = findings;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getWeatherConditions() {
        return weatherConditions;
    }
    
    public void setWeatherConditions(String weatherConditions) {
        this.weatherConditions = weatherConditions;
    }
    
    public String[] getAttendees() {
        return attendees;
    }
    
    public void setAttendees(String[] attendees) {
        this.attendees = attendees;
    }
}

