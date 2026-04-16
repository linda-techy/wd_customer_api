package com.wd.custapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Read-only mirror of the portal-owned {@code boq_items} table.
 *
 * <p>Customer API never writes to this table — portal API owns the schema and all
 * mutations. Every column is mapped as {@code insertable=false, updatable=false}
 * so that even if a dirty entity is accidentally flushed, Hibernate will not
 * issue an INSERT or UPDATE.
 *
 * <p>Fields that exist in the table but are not useful for customer-facing reads
 * (e.g., {@code material_id}, audit user IDs) are still mapped here for schema
 * completeness — Hibernate's {@code validate} mode requires all columns to be
 * known, and unmapped columns in the table would cause a startup failure if the
 * team ever switches from {@code ddl-auto=create} to {@code validate}.
 */
@Entity
@Table(name = "boq_items")
@Where(clause = "deleted_at IS NULL")
public class BoqItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, insertable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_type_id", insertable = false, updatable = false)
    private BoqWorkType workType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private BoqCategory category;

    @Column(name = "material_id", insertable = false, updatable = false)
    private Long materialId;

    @Column(name = "item_code", length = 50, insertable = false, updatable = false)
    private String itemCode;

    @Column(nullable = false, length = 255, insertable = false, updatable = false)
    private String description;

    @Column(length = 50, insertable = false, updatable = false)
    private String unit;

    @Column(precision = 18, scale = 6, insertable = false, updatable = false)
    private BigDecimal quantity;

    @Column(name = "unit_rate", precision = 18, scale = 6, insertable = false, updatable = false)
    private BigDecimal rate;

    @Column(name = "total_amount", precision = 18, scale = 6, insertable = false, updatable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT", insertable = false, updatable = false)
    private String specifications;

    @Column(columnDefinition = "TEXT", insertable = false, updatable = false)
    private String notes;

    @Column(name = "executed_quantity", precision = 18, scale = 6, insertable = false, updatable = false)
    private BigDecimal executedQuantity = BigDecimal.ZERO;

    @Column(name = "billed_quantity", precision = 18, scale = 6, insertable = false, updatable = false)
    private BigDecimal billedQuantity = BigDecimal.ZERO;

    @Column(name = "status", length = 20, insertable = false, updatable = false)
    private String status = "DRAFT";

    @Column(name = "is_active", insertable = false, updatable = false)
    private Boolean isActive = true;

    @Column(name = "boq_document_id", insertable = false, updatable = false)
    private Long boqDocumentId;

    @Column(name = "item_kind", length = 20, insertable = false, updatable = false)
    private String itemKind = "BASE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by_user_id", insertable = false, updatable = false)
    private Long createdByUserId;

    @Column(name = "updated_by_user_id", insertable = false, updatable = false)
    private Long updatedByUserId;

    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_user_id", insertable = false, updatable = false)
    private Long deletedByUserId;

    @Column(name = "version", insertable = false, updatable = false)
    private Long version;

    public BoqItem() {}

    // Getters (no setters — entity is fully read-only)

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public BoqWorkType getWorkType() { return workType; }
    public BoqCategory getCategory() { return category; }
    public Long getMaterialId() { return materialId; }
    public String getItemCode() { return itemCode; }
    public String getDescription() { return description; }
    public String getUnit() { return unit; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getRate() { return rate; }

    public BigDecimal getAmount() {
        if (amount == null && quantity != null && rate != null) {
            return quantity.multiply(rate).setScale(6, RoundingMode.HALF_UP);
        }
        return amount;
    }

    public String getSpecifications() { return specifications; }
    public String getNotes() { return notes; }
    public BigDecimal getExecutedQuantity() { return executedQuantity != null ? executedQuantity : BigDecimal.ZERO; }
    public BigDecimal getBilledQuantity() { return billedQuantity != null ? billedQuantity : BigDecimal.ZERO; }
    public String getStatus() { return status != null ? status : "DRAFT"; }
    public Boolean getIsActive() { return isActive; }
    public Long getBoqDocumentId() { return boqDocumentId; }
    public String getItemKind() { return itemKind != null ? itemKind : "BASE"; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public Long getUpdatedByUserId() { return updatedByUserId; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public Long getDeletedByUserId() { return deletedByUserId; }
    public Long getVersion() { return version; }

    // Computed getters for customer view

    @Transient
    public BigDecimal getRemainingQuantity() {
        BigDecimal q = quantity != null ? quantity : BigDecimal.ZERO;
        BigDecimal remaining = q.subtract(getExecutedQuantity());
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    @Transient
    public BigDecimal getTotalExecutedAmount() {
        if (getExecutedQuantity() != null && rate != null) {
            return getExecutedQuantity().multiply(rate).setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public BigDecimal getTotalBilledAmount() {
        if (getBilledQuantity() != null && rate != null) {
            return getBilledQuantity().multiply(rate).setScale(6, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public BigDecimal getExecutionPercentage() {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
            return getExecutedQuantity().divide(quantity, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public BigDecimal getBillingPercentage() {
        if (getExecutedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            return getBilledQuantity().divide(getExecutedQuantity(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}
