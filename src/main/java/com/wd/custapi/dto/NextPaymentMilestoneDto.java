package com.wd.custapi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Customer-facing payload for {@code GET /api/projects/{uuid}/boq/payment-schedule?nextOnly=true}.
 *
 * <p>{@code stage} is {@code null} when no non-terminal stage exists
 * (all stages are PAID or ON_HOLD) — the customer-app card hides on null.
 *
 * <p>{@code stage.daysUntilDue} is server-computed against
 * {@code LocalDate.now(ZoneId.of("Asia/Kolkata"))} and is {@code null}
 * when {@code stage.dueDate} is {@code null}. Negative values mean overdue.
 *
 * <p>{@code stage.percentOfContract} is recomputed defensively as
 * {@code (netPayableAmount / summary.totalContractValue) * 100} rounded
 * to 1 decimal — guards against future contract-total drift if change
 * orders alter the headline figure without touching {@code stage_percentage}.
 */
public record NextPaymentMilestoneDto(
        Stage stage,
        Summary summary
) {
    public record Stage(
            Integer stageNumber,
            String stageName,
            LocalDate dueDate,
            Integer daysUntilDue,
            String status,
            BigDecimal netPayableAmount,
            BigDecimal stagePercentage,
            BigDecimal percentOfContract,
            Integer totalStages
    ) {}

    public record Summary(
            BigDecimal totalContractValue,
            BigDecimal totalPaid,
            BigDecimal totalOutstanding,
            Integer stageCount
    ) {}
}
