package com.wd.custapi.model;

import com.wd.custapi.model.enums.InvoiceStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Read-only view of boq_invoices table for customer-facing API.
 * Covers both STAGE_INVOICE and CO_INVOICE types (Method 2 billing).
 * Portal API manages all mutations.
 */
@Immutable
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "boq_invoices")
public class BoqInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "invoice_type", length = 20)
    private String invoiceType;

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;

    @Column(name = "subtotal_ex_gst", precision = 18, scale = 6)
    private BigDecimal subtotalExGst;

    @Column(name = "gst_rate", precision = 5, scale = 4)
    private BigDecimal gstRate;

    @Column(name = "gst_amount", precision = 18, scale = 6)
    private BigDecimal gstAmount;

    @Column(name = "total_incl_gst", precision = 18, scale = 6)
    private BigDecimal totalInclGst;

    @Column(name = "total_credit_applied", precision = 18, scale = 6)
    private BigDecimal totalCreditApplied;

    @Column(name = "net_amount_due", precision = 18, scale = 6)
    private BigDecimal netAmountDue;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private InvoiceStatus status;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters — no setters (read-only entity)
    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getInvoiceType() { return invoiceType; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public BigDecimal getSubtotalExGst() { return subtotalExGst; }
    public BigDecimal getGstRate() { return gstRate; }
    public BigDecimal getGstAmount() { return gstAmount; }
    public BigDecimal getTotalInclGst() { return totalInclGst; }
    public BigDecimal getTotalCreditApplied() { return totalCreditApplied; }
    public BigDecimal getNetAmountDue() { return netAmountDue; }
    public InvoiceStatus getStatus() { return status; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
