package com.wd.custapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only entity mapping for design_package_payments table.
 * Used by customer API to resolve payment -> project relationships.
 */
@Entity
@Table(name = "design_package_payments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DesignPackagePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "package_name", length = 50)
    private String packageName;

    @Column(name = "rate_per_sqft", precision = 10, scale = 2)
    private BigDecimal ratePerSqft;

    @Column(name = "total_sqft", precision = 10, scale = 2)
    private BigDecimal totalSqft;

    @Column(name = "base_amount", precision = 15, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    private BigDecimal gstPercentage;

    @Column(name = "gst_amount", precision = 15, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "payment_type", length = 20)
    private String paymentType;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "retention_percentage", precision = 5, scale = 2)
    private BigDecimal retentionPercentage;

    @Column(name = "retention_amount", precision = 15, scale = 2)
    private BigDecimal retentionAmount;

    @Column(name = "retention_released_amount", precision = 15, scale = 2)
    private BigDecimal retentionReleasedAmount;

    @Column(name = "defect_liability_end_date")
    private java.time.LocalDate defectLiabilityEndDate;

    @Column(name = "retention_status", length = 20)
    private String retentionStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters
    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getPackageName() { return packageName; }
    public BigDecimal getRatePerSqft() { return ratePerSqft; }
    public BigDecimal getTotalSqft() { return totalSqft; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public BigDecimal getGstPercentage() { return gstPercentage; }
    public BigDecimal getGstAmount() { return gstAmount; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getPaymentType() { return paymentType; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public BigDecimal getRetentionPercentage() { return retentionPercentage; }
    public BigDecimal getRetentionAmount() { return retentionAmount; }
    public BigDecimal getRetentionReleasedAmount() { return retentionReleasedAmount; }
    public java.time.LocalDate getDefectLiabilityEndDate() { return defectLiabilityEndDate; }
    public String getRetentionStatus() { return retentionStatus; }
}
