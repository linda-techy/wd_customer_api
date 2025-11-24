package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gallery_images")
public class GalleryImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;
    
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;
    
    @Column(columnDefinition = "TEXT")
    private String caption;
    
    @Column(name = "taken_date", nullable = false)
    private LocalDate takenDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private CustomerUser uploadedBy;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_report_id")
    private SiteReport siteReport;
    
    @Column(name = "location_tag", length = 255)
    private String locationTag;
    
    @Column(columnDefinition = "varchar(255)[]")
    private String[] tags;
    
    // Constructors
    public GalleryImage() {}
    
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
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public String getThumbnailPath() {
        return thumbnailPath;
    }
    
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
    
    public String getCaption() {
        return caption;
    }
    
    public void setCaption(String caption) {
        this.caption = caption;
    }
    
    public LocalDate getTakenDate() {
        return takenDate;
    }
    
    public void setTakenDate(LocalDate takenDate) {
        this.takenDate = takenDate;
    }
    
    public CustomerUser getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(CustomerUser uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public SiteReport getSiteReport() {
        return siteReport;
    }
    
    public void setSiteReport(SiteReport siteReport) {
        this.siteReport = siteReport;
    }
    
    public String getLocationTag() {
        return locationTag;
    }
    
    public void setLocationTag(String locationTag) {
        this.locationTag = locationTag;
    }
    
    public String[] getTags() {
        return tags;
    }
    
    public void setTags(String[] tags) {
        this.tags = tags;
    }
}

