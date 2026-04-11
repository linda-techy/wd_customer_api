package com.wd.custapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Customer BOQ approval / change request record.
 *
 * Append-only — records are never updated or deleted.
 * The latest record per project is the current approval status.
 *
 * status values:
 *   APPROVED         — customer confirms the BOQ
 *   CHANGE_REQUESTED — customer requests modifications
 */
@Entity
@Table(name = "boq_approvals")
public class BoqApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public BoqApproval() {}

    public BoqApproval(Long projectId, Long customerUserId, String status, String message) {
        this.projectId = projectId;
        this.customerUserId = customerUserId;
        this.status = status;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getCustomerUserId() { return customerUserId; }
    public void setCustomerUserId(Long customerUserId) { this.customerUserId = customerUserId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
