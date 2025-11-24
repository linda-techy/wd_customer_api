package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cctv_cameras")
public class CctvCamera {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(name = "camera_name", nullable = false, length = 100)
    private String cameraName;
    
    @Column(length = 255)
    private String location;
    
    @Column(name = "stream_url", length = 500)
    private String streamUrl;
    
    @Column(name = "snapshot_url", length = 500)
    private String snapshotUrl;
    
    @Column(name = "is_installed")
    private Boolean isInstalled = false;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "installation_date")
    private LocalDate installationDate;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    @Column(name = "camera_type", length = 50)
    private String cameraType;
    
    @Column(length = 20)
    private String resolution;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    // Constructors
    public CctvCamera() {}
    
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
    
    public String getCameraName() {
        return cameraName;
    }
    
    public void setCameraName(String cameraName) {
        this.cameraName = cameraName;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getStreamUrl() {
        return streamUrl;
    }
    
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }
    
    public String getSnapshotUrl() {
        return snapshotUrl;
    }
    
    public void setSnapshotUrl(String snapshotUrl) {
        this.snapshotUrl = snapshotUrl;
    }
    
    public Boolean getIsInstalled() {
        return isInstalled;
    }
    
    public void setIsInstalled(Boolean isInstalled) {
        this.isInstalled = isInstalled;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDate getInstallationDate() {
        return installationDate;
    }
    
    public void setInstallationDate(LocalDate installationDate) {
        this.installationDate = installationDate;
    }
    
    public LocalDateTime getLastActive() {
        return lastActive;
    }
    
    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }
    
    public String getCameraType() {
        return cameraType;
    }
    
    public void setCameraType(String cameraType) {
        this.cameraType = cameraType;
    }
    
    public String getResolution() {
        return resolution;
    }
    
    public void setResolution(String resolution) {
        this.resolution = resolution;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}

