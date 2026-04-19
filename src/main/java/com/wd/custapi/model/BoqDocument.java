package com.wd.custapi.model;

import com.wd.custapi.model.enums.BoqDocumentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only view of the boq_documents table for the customer-facing API.
 * Mutations are performed only by the portal API.
 */
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "boq_documents")
public class BoqDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "total_value_ex_gst", precision = 18, scale = 6)
    private BigDecimal totalValueExGst;

    @Column(name = "gst_rate", precision = 5, scale = 4)
    private BigDecimal gstRate;

    @Column(name = "total_gst_amount", precision = 18, scale = 6)
    private BigDecimal totalGstAmount;

    @Column(name = "total_value_incl_gst", precision = 18, scale = 6)
    private BigDecimal totalValueInclGst;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BoqDocumentStatus status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "customer_approved_at")
    private LocalDateTime customerApprovedAt;

    @Column(name = "customer_approved_by")
    private Long customerApprovedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "revision_number")
    private Integer revisionNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "customer_acknowledged_at")
    private LocalDateTime customerAcknowledgedAt;

    @Column(name = "customer_acknowledged_by")
    private Long customerAcknowledgedBy;

    // ---- Getters ----

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public BigDecimal getTotalValueExGst() { return totalValueExGst; }
    public BigDecimal getGstRate() { return gstRate; }
    public BigDecimal getTotalGstAmount() { return totalGstAmount; }
    public BigDecimal getTotalValueInclGst() { return totalValueInclGst; }
    public BoqDocumentStatus getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getCustomerApprovedAt() { return customerApprovedAt; }
    public Long getCustomerApprovedBy() { return customerApprovedBy; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public Integer getRevisionNumber() { return revisionNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCustomerAcknowledgedAt() { return customerAcknowledgedAt; }
    public Long getCustomerAcknowledgedBy() { return customerAcknowledgedBy; }
}
