package com.wd.custapi.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only view of payment_stages for the customer-facing API.
 */
@Entity
@Table(name = "payment_stages")
public class PaymentStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boq_document_id")
    private BoqDocument boqDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "stage_number")
    private Integer stageNumber;

    @Column(name = "stage_name", length = 100)
    private String stageName;

    @Column(name = "boq_value_snapshot", precision = 18, scale = 6)
    private BigDecimal boqValueSnapshot;

    @Column(name = "stage_percentage", precision = 6, scale = 4)
    private BigDecimal stagePercentage;

    @Column(name = "stage_amount_ex_gst", precision = 18, scale = 6)
    private BigDecimal stageAmountExGst;

    @Column(name = "gst_rate", precision = 5, scale = 4)
    private BigDecimal gstRate;

    @Column(name = "gst_amount", precision = 18, scale = 6)
    private BigDecimal gstAmount;

    @Column(name = "stage_amount_incl_gst", precision = 18, scale = 6)
    private BigDecimal stageAmountInclGst;

    @Column(name = "applied_credit_amount", precision = 18, scale = 6)
    private BigDecimal appliedCreditAmount;

    @Column(name = "net_payable_amount", precision = 18, scale = 6)
    private BigDecimal netPayableAmount;

    @Column(name = "paid_amount", precision = 18, scale = 6)
    private BigDecimal paidAmount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "milestone_description", columnDefinition = "TEXT")
    private String milestoneDescription;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ---- Certification / retention (added V25) — read-only for customer ----

    @Column(name = "certified_by", length = 100)
    private String certifiedBy;

    @Column(name = "retention_pct", precision = 5, scale = 4)
    private java.math.BigDecimal retentionPct;

    @Column(name = "retention_held", precision = 18, scale = 6)
    private java.math.BigDecimal retentionHeld;

    @Column(name = "certified_at")
    private LocalDateTime certifiedAt;

    // ---- Getters ----

    public Long getId() { return id; }
    public BoqDocument getBoqDocument() { return boqDocument; }
    public Project getProject() { return project; }
    public Integer getStageNumber() { return stageNumber; }
    public String getStageName() { return stageName; }
    public BigDecimal getBoqValueSnapshot() { return boqValueSnapshot; }
    public BigDecimal getStagePercentage() { return stagePercentage; }
    public BigDecimal getStageAmountExGst() { return stageAmountExGst; }
    public BigDecimal getGstRate() { return gstRate; }
    public BigDecimal getGstAmount() { return gstAmount; }
    public BigDecimal getStageAmountInclGst() { return stageAmountInclGst; }
    public BigDecimal getAppliedCreditAmount() { return appliedCreditAmount; }
    public BigDecimal getNetPayableAmount() { return netPayableAmount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public String getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public String getMilestoneDescription() { return milestoneDescription; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getCertifiedBy() { return certifiedBy; }
    public java.math.BigDecimal getRetentionPct() { return retentionPct; }
    public java.math.BigDecimal getRetentionHeld() { return retentionHeld; }
    public LocalDateTime getCertifiedAt() { return certifiedAt; }
}
