package com.wd.custapi.dto;

import com.wd.custapi.model.PaymentSchedule;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer-safe DTO for Payment Schedules.
 * Excludes sensitive company information like:
 * - Retention percentages and amounts (company strategy)
 * - Internal accounting details (TDS, net amounts)
 * - Company profit margins
 * - Internal reference numbers
 */
public class CustomerPaymentScheduleDto {

    private Long id;
    private Integer installmentNumber;
    private String description;
    private BigDecimal amount;
    private String dueDate;
    private String status;
    private BigDecimal paidAmount;
    private String paidDate;
    private List<CustomerPaymentTransactionDto> transactions;

    public CustomerPaymentScheduleDto(PaymentSchedule schedule) {
        this.id = schedule.getId();
        this.installmentNumber = schedule.getInstallmentNumber();
        this.description = schedule.getDescription();
        this.amount = schedule.getAmount();
        this.dueDate = schedule.getDueDate() != null ? schedule.getDueDate().toString() : null;
        this.status = schedule.getStatus();
        this.paidAmount = schedule.getPaidAmount();
        this.paidDate = schedule.getPaidDate() != null ? schedule.getPaidDate().toString() : null;

        if (schedule.getTransactions() != null) {
            this.transactions = schedule.getTransactions().stream()
                    .map(CustomerPaymentTransactionDto::new)
                    .collect(Collectors.toList());
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public String getPaidDate() { return paidDate; }
    public void setPaidDate(String paidDate) { this.paidDate = paidDate; }

    public List<CustomerPaymentTransactionDto> getTransactions() { return transactions; }
    public void setTransactions(List<CustomerPaymentTransactionDto> transactions) { this.transactions = transactions; }
}
