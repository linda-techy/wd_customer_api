package com.wd.custapi.dto;

import com.wd.custapi.model.ProjectInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer-facing invoice DTO.
 * Does NOT expose internal notes or draft invoices.
 * Financial details shown only to CUSTOMER / ADMIN roles (enforced at controller level).
 */
public class CustomerInvoiceDto {

    private final Long id;
    private final String invoiceNumber;
    private final LocalDate invoiceDate;
    private final LocalDate dueDate;
    private final BigDecimal subTotal;
    private final BigDecimal gstAmount;
    private final BigDecimal totalAmount;
    private final String status;
    private final LocalDateTime createdAt;

    public CustomerInvoiceDto(ProjectInvoice invoice) {
        this.id = invoice.getId();
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.invoiceDate = invoice.getInvoiceDate();
        this.dueDate = invoice.getDueDate();
        this.subTotal = invoice.getSubTotal();
        this.gstAmount = invoice.getGstAmount();
        this.totalAmount = invoice.getTotalAmount();
        this.status = invoice.getStatus();
        this.createdAt = invoice.getCreatedAt();
    }

    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getSubTotal() { return subTotal; }
    public BigDecimal getGstAmount() { return gstAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
