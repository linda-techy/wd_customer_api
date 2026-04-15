package com.wd.custapi.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only view of deduction_register for the customer-facing API.
 * Customers can see the status of deductions raised against their project.
 */
@Entity
@Table(name = "deduction_register")
public class DeductionRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_id")
    private ChangeOrder changeOrder;

    @Column(name = "item_description", columnDefinition = "TEXT")
    private String itemDescription;

    @Column(name = "requested_amount", precision = 18, scale = 6)
    private BigDecimal requestedAmount;

    @Column(name = "accepted_amount", precision = 18, scale = 6)
    private BigDecimal acceptedAmount;

    @Column(name = "decision", length = 30)
    private String decision;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "escalation_status", length = 20)
    private String escalationStatus;

    @Column(name = "settled_in_final_account")
    private Boolean settledInFinalAccount;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "decision_date")
    private LocalDate decisionDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Getters ----

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public ChangeOrder getChangeOrder() { return changeOrder; }
    public String getItemDescription() { return itemDescription; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getAcceptedAmount() { return acceptedAmount; }
    public String getDecision() { return decision; }
    public String getRejectionReason() { return rejectionReason; }
    public String getEscalationStatus() { return escalationStatus; }
    public Boolean getSettledInFinalAccount() { return settledInFinalAccount; }
    public String getApprovedBy() { return approvedBy; }
    public LocalDate getDecisionDate() { return decisionDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
