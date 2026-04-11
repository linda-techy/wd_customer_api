package com.wd.custapi.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "boq_items")
@Where(clause = "deleted_at IS NULL")
public class BoqItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_type_id", nullable = false)
    private BoqWorkType workType;
    
    @Column(name = "item_code", length = 50)
    private String itemCode;
    
    @Column(nullable = false, length = 255)
    private String description;
    
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "unit_rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;

    // Amount is stored as total_amount (quantity * unit_rate), managed by Portal API
    @Column(name = "total_amount", precision = 18, scale = 6, insertable = false, updatable = false)
    private BigDecimal amount;
    
    @Column(columnDefinition = "TEXT")
    private String specifications;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // created_by_user_id is written by Portal API's BaseEntity @CreatedBy (PortalUser ID).
    // We expose it as a plain Long — no FK join since it references portal_users, not customer_users.
    @Column(name = "created_by_user_id", insertable = false, updatable = false)
    private Long createdByUserId;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    // Financial tracking fields (READ-ONLY for customers, managed by Portal API)
    @Column(name = "executed_quantity", precision = 18, scale = 6, insertable = false, updatable = false)
    private BigDecimal executedQuantity = BigDecimal.ZERO;

    @Column(name = "billed_quantity", precision = 18, scale = 6, insertable = false, updatable = false)
    private BigDecimal billedQuantity = BigDecimal.ZERO;
    
    @Column(name = "status", length = 20, insertable = false, updatable = false)
    private String status = "DRAFT";
    
    @Column(name = "deleted_at", insertable = false, updatable = false)
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private BoqCategory category;
    
    // Constructors
    public BoqItem() {}
    
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
    
    public BoqWorkType getWorkType() {
        return workType;
    }
    
    public void setWorkType(BoqWorkType workType) {
        this.workType = workType;
    }
    
    public String getItemCode() {
        return itemCode;
    }
    
    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getQuantity() {
        return quantity;
    }
    
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public BigDecimal getRate() {
        return rate;
    }
    
    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }
    
    public BigDecimal getAmount() {
        // Compute on the fly if not set
        if (amount == null && quantity != null && rate != null) {
            return quantity.multiply(rate);
        }
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getSpecifications() {
        return specifications;
    }
    
    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getCreatedByUserId() {
        return createdByUserId;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public BigDecimal getExecutedQuantity() {
        return executedQuantity != null ? executedQuantity : BigDecimal.ZERO;
    }
    
    public void setExecutedQuantity(BigDecimal executedQuantity) {
        this.executedQuantity = executedQuantity;
    }
    
    public BigDecimal getBilledQuantity() {
        return billedQuantity != null ? billedQuantity : BigDecimal.ZERO;
    }
    
    public void setBilledQuantity(BigDecimal billedQuantity) {
        this.billedQuantity = billedQuantity;
    }
    
    public String getStatus() {
        return status != null ? status : "DRAFT";
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BoqCategory getCategory() {
        return category;
    }
    
    public void setCategory(BoqCategory category) {
        this.category = category;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    // Computed getters for customer view
    @Transient
    public BigDecimal getRemainingQuantity() {
        BigDecimal remaining = quantity.subtract(getExecutedQuantity());
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }
    
    @Transient
    public BigDecimal getTotalExecutedAmount() {
        if (getExecutedQuantity() != null && rate != null) {
            return getExecutedQuantity().multiply(rate);
        }
        return BigDecimal.ZERO;
    }
    
    @Transient
    public BigDecimal getTotalBilledAmount() {
        if (getBilledQuantity() != null && rate != null) {
            return getBilledQuantity().multiply(rate);
        }
        return BigDecimal.ZERO;
    }
    
    @Transient
    public BigDecimal getExecutionPercentage() {
        if (quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0) {
            return getExecutedQuantity().divide(quantity, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
    
    @Transient
    public BigDecimal getBillingPercentage() {
        if (getExecutedQuantity().compareTo(BigDecimal.ZERO) > 0) {
            return getBilledQuantity().divide(getExecutedQuantity(), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

