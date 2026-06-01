package com.wd.custapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "view_360")
public class View360 {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @Column(nullable = false, length = 255)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "view_url", nullable = false, length = 500)
    private String viewUrl;
    
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
    
    // Shared view_360.capture_date is TIMESTAMP (portal V147); map as LocalDateTime
    // to match the column. The customer API contract still exposes it as a date —
    // View360Service converts at the boundary.
    @Column(name = "capture_date")
    private LocalDateTime captureDate;
    
    @Column(length = 255)
    private String location;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private CustomerUser uploadedBy;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "view_count")
    private Integer viewCount = 0;
    
    // Constructors
    public View360() {
        // Default constructor required by JPA
    }
    
    // Getters and Setters
    // Boolean isActive accessors kept manual (Lombok skips fields with existing accessors)
    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}

