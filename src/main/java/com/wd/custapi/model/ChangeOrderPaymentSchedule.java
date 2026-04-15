package com.wd.custapi.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only view of co_payment_schedule for the customer-facing API.
 * Shows advance / progress / completion tranche amounts and status.
 */
@Entity
@Table(name = "co_payment_schedule")
public class ChangeOrderPaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_id")
    private ChangeOrder changeOrder;

    @Column(name = "advance_pct")
    private Integer advancePct;

    @Column(name = "advance_amount", precision = 18, scale = 6)
    private BigDecimal advanceAmount;

    @Column(name = "advance_status", length = 20)
    private String advanceStatus;

    @Column(name = "advance_due_date")
    private LocalDate advanceDueDate;

    @Column(name = "advance_paid_date")
    private LocalDate advancePaidDate;

    @Column(name = "advance_invoice_number", length = 50)
    private String advanceInvoiceNumber;

    @Column(name = "progress_pct")
    private Integer progressPct;

    @Column(name = "progress_amount", precision = 18, scale = 6)
    private BigDecimal progressAmount;

    @Column(name = "progress_status", length = 20)
    private String progressStatus;

    @Column(name = "progress_paid_date")
    private LocalDate progressPaidDate;

    @Column(name = "completion_pct")
    private Integer completionPct;

    @Column(name = "completion_amount", precision = 18, scale = 6)
    private BigDecimal completionAmount;

    @Column(name = "completion_status", length = 20)
    private String completionStatus;

    @Column(name = "completion_trigger", length = 50)
    private String completionTrigger;

    @Column(name = "completion_paid_date")
    private LocalDate completionPaidDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Getters ----

    public Long getId() { return id; }
    public ChangeOrder getChangeOrder() { return changeOrder; }
    public Integer getAdvancePct() { return advancePct; }
    public BigDecimal getAdvanceAmount() { return advanceAmount; }
    public String getAdvanceStatus() { return advanceStatus; }
    public LocalDate getAdvanceDueDate() { return advanceDueDate; }
    public LocalDate getAdvancePaidDate() { return advancePaidDate; }
    public String getAdvanceInvoiceNumber() { return advanceInvoiceNumber; }
    public Integer getProgressPct() { return progressPct; }
    public BigDecimal getProgressAmount() { return progressAmount; }
    public String getProgressStatus() { return progressStatus; }
    public LocalDate getProgressPaidDate() { return progressPaidDate; }
    public Integer getCompletionPct() { return completionPct; }
    public BigDecimal getCompletionAmount() { return completionAmount; }
    public String getCompletionStatus() { return completionStatus; }
    public String getCompletionTrigger() { return completionTrigger; }
    public LocalDate getCompletionPaidDate() { return completionPaidDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
