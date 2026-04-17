package com.wd.custapi.dto;

import com.wd.custapi.model.BoqInvoice;
import com.wd.custapi.model.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerBoqInvoiceDto(
        Long id,
        String invoiceNumber,
        String invoiceType,
        BigDecimal subtotalExGst,
        BigDecimal gstRate,
        BigDecimal gstAmount,
        BigDecimal totalInclGst,
        BigDecimal totalCreditApplied,
        BigDecimal netAmountDue,
        InvoiceStatus status,
        LocalDate issueDate,
        LocalDate dueDate,
        LocalDateTime sentAt,
        LocalDateTime paidAt
) {
    public static CustomerBoqInvoiceDto from(BoqInvoice inv) {
        return new CustomerBoqInvoiceDto(
                inv.getId(), inv.getInvoiceNumber(), inv.getInvoiceType(),
                inv.getSubtotalExGst(), inv.getGstRate(), inv.getGstAmount(),
                inv.getTotalInclGst(), inv.getTotalCreditApplied(), inv.getNetAmountDue(),
                inv.getStatus(), inv.getIssueDate(), inv.getDueDate(),
                inv.getSentAt(), inv.getPaidAt()
        );
    }
}
