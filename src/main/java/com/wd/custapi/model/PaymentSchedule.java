package com.wd.custapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only entity mapping for payment_schedule table.
 * Customer API only reads payment data; portal API manages it.
 */
@Entity
@Table(name = "payment_schedule")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "design_payment_id", nullable = false)
    private DesignPackagePayment designPayment;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "description", nullable = false, length = 100)
    private String description;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL)
    private List<PaymentTransaction> transactions = new ArrayList<>();

    // Getters
    public Long getId() {
        return id;
    }

    public DesignPackagePayment getDesignPayment() {
        return designPayment;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public LocalDateTime getPaidDate() {
        return paidDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<PaymentTransaction> getTransactions() {
        return transactions;
    }
}
