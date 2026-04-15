package com.wd.custapi.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only view of final_account for the customer-facing API.
 * Exposes the project financial reconciliation to the customer.
 */
@Entity
@Table(name = "final_account")
public class FinalAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "base_contract_value", precision = 18, scale = 6)
    private BigDecimal baseContractValue;

    @Column(name = "total_additions", precision = 18, scale = 6)
    private BigDecimal totalAdditions;

    @Column(name = "total_accepted_deductions", precision = 18, scale = 6)
    private BigDecimal totalAcceptedDeductions;

    @Column(name = "total_received_to_date", precision = 18, scale = 6)
    private BigDecimal totalReceivedToDate;

    @Column(name = "total_retention_held", precision = 18, scale = 6)
    private BigDecimal totalRetentionHeld;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "dlp_start_date")
    private LocalDate dlpStartDate;

    @Column(name = "dlp_end_date")
    private LocalDate dlpEndDate;

    @Column(name = "retention_released")
    private Boolean retentionReleased;

    @Column(name = "retention_release_date")
    private LocalDate retentionReleaseDate;

    @Column(name = "prepared_by", length = 100)
    private String preparedBy;

    @Column(name = "agreed_by", length = 100)
    private String agreedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Computed helpers ----

    @Transient
    public BigDecimal getNetRevisedContractValue() {
        BigDecimal base = baseContractValue != null ? baseContractValue : BigDecimal.ZERO;
        BigDecimal add  = totalAdditions != null ? totalAdditions : BigDecimal.ZERO;
        BigDecimal ded  = totalAcceptedDeductions != null ? totalAcceptedDeductions : BigDecimal.ZERO;
        return base.add(add).subtract(ded);
    }

    @Transient
    public BigDecimal getBalancePayable() {
        BigDecimal net  = getNetRevisedContractValue();
        BigDecimal paid = totalReceivedToDate != null ? totalReceivedToDate : BigDecimal.ZERO;
        return net.subtract(paid);
    }

    // ---- Getters ----

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public BigDecimal getBaseContractValue() { return baseContractValue; }
    public BigDecimal getTotalAdditions() { return totalAdditions; }
    public BigDecimal getTotalAcceptedDeductions() { return totalAcceptedDeductions; }
    public BigDecimal getTotalReceivedToDate() { return totalReceivedToDate; }
    public BigDecimal getTotalRetentionHeld() { return totalRetentionHeld; }
    public String getStatus() { return status; }
    public LocalDate getDlpStartDate() { return dlpStartDate; }
    public LocalDate getDlpEndDate() { return dlpEndDate; }
    public Boolean getRetentionReleased() { return retentionReleased; }
    public LocalDate getRetentionReleaseDate() { return retentionReleaseDate; }
    public String getPreparedBy() { return preparedBy; }
    public String getAgreedBy() { return agreedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
