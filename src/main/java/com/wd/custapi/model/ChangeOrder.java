package com.wd.custapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only view of change_orders for the customer-facing API.
 * Customer can approve or reject via dedicated service methods.
 */
@SQLDelete(sql = "UPDATE change_orders SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@Entity
@Table(name = "change_orders")
public class ChangeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "reference_number", length = 50)
    private String referenceNumber;

    @Column(name = "co_type", length = 40)
    private String coType;

    @Column(nullable = false, length = 25)
    private String status;

    @Column(name = "net_amount_ex_gst", precision = 18, scale = 6)
    private BigDecimal netAmountExGst;

    @Column(name = "gst_rate", precision = 5, scale = 4)
    private BigDecimal gstRate;

    @Column(name = "gst_amount", precision = 18, scale = 6)
    private BigDecimal gstAmount;

    @Column(name = "net_amount_incl_gst", precision = 18, scale = 6)
    private BigDecimal netAmountInclGst;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "customer_reviewed_at")
    private LocalDateTime customerReviewedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // ---- VO classification fields (added V25) — read-only for customer ----

    @Column(name = "vo_category", length = 30)
    private String voCategory;

    @Column(name = "scope_notes", columnDefinition = "TEXT")
    private String scopeNotes;

    @Column(name = "approved_cost", precision = 18, scale = 6)
    private java.math.BigDecimal approvedCost;

    @Column(name = "advance_collected")
    private Boolean advanceCollected;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Version
    @Column(name = "version")
    private Long version;

    // ---- Setters for approval/rejection (called by CustomerChangeOrderService) ----

    public void setStatus(String status) { this.status = status; }
    public void setCustomerReviewedAt(LocalDateTime t) { this.customerReviewedAt = t; }
    public void setApprovedAt(LocalDateTime t) { this.approvedAt = t; }
    public void setApprovedBy(Long id) { this.approvedBy = id; }
    public void setRejectedAt(LocalDateTime t) { this.rejectedAt = t; }
    public void setRejectedBy(Long id) { this.rejectedBy = id; }
    public void setRejectionReason(String r) { this.rejectionReason = r; }

    // ---- Getters ----

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getCoType() { return coType; }
    public String getStatus() { return status; }
    public BigDecimal getNetAmountExGst() { return netAmountExGst; }
    public BigDecimal getGstRate() { return gstRate; }
    public BigDecimal getGstAmount() { return gstAmount; }
    public BigDecimal getNetAmountInclGst() { return netAmountInclGst; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getJustification() { return justification; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getCustomerReviewedAt() { return customerReviewedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public Long getApprovedBy() { return approvedBy; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public Long getRejectedBy() { return rejectedBy; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getVersion() { return version; }
    public String getVoCategory() { return voCategory; }
    public String getScopeNotes() { return scopeNotes; }
    public java.math.BigDecimal getApprovedCost() { return approvedCost; }
    public Boolean getAdvanceCollected() { return advanceCollected; }
}
