package com.wd.custapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only entity mapping for payment_transactions table.
 * Customer API only reads; portal API manages transactions.
 * Sensitive fields (TDS, net amount, internal notes) are excluded from DTOs.
 */
@Entity
@Table(name = "payment_transactions")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private PaymentSchedule schedule;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "receipt_number", length = 50)
    private String receiptNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Getters (read-only - no setters needed for customer API)
    public Long getId() {
        return id;
    }

    public PaymentSchedule getSchedule() {
        return schedule;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
