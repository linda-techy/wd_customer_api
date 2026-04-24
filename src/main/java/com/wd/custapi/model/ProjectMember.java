package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Read-only mapping of the portal-owned project_members table.
 * The portal API owns writes; customer API only reads visibility-gated
 * subset for the project info team card.
 */
@Entity
@Table(name = "project_members")
public class ProjectMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "portal_user_id")
    private Long portalUserId;

    @Column(name = "customer_user_id")
    private Long customerUserId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "role_in_project", length = 50)
    private String roleInProject;

    @Column(name = "share_with_customer", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private Boolean shareWithCustomer = Boolean.FALSE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public Long getProjectId() { return projectId; }
    public Long getPortalUserId() { return portalUserId; }
    public Long getCustomerUserId() { return customerUserId; }
    public Long getCustomerId() { return customerId; }
    public String getRoleInProject() { return roleInProject; }
    public Boolean getShareWithCustomer() { return shareWithCustomer; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
