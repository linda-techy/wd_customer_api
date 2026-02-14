package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * BOQ Category entity (READ-ONLY for customers).
 * Categories are managed from the portal side.
 */
@Entity
@Table(name = "boq_categories")
public class BoqCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, insertable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private BoqCategory parent;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public BoqCategory() {}

    // Getters only (read-only for customers)

    public Long getId() { return id; }

    public Project getProject() { return project; }

    public BoqCategory getParent() { return parent; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public Integer getDisplayOrder() { return displayOrder; }

    public Boolean getIsActive() { return isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @Transient
    public boolean isTopLevel() {
        return parent == null;
    }
}
