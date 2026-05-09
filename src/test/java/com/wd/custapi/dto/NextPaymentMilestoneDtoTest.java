package com.wd.custapi.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NextPaymentMilestoneDtoTest {

    @Test
    void recordCarriesNullableStageAndRequiredSummary() {
        NextPaymentMilestoneDto.Stage stage = new NextPaymentMilestoneDto.Stage(
                4, "Plastering", LocalDate.of(2026, 5, 15), 5,
                "DUE", new BigDecimal("425000.00"),
                new BigDecimal("12.0"), new BigDecimal("12.0"), 7);
        NextPaymentMilestoneDto.Summary summary = new NextPaymentMilestoneDto.Summary(
                new BigDecimal("3500000.00"),
                new BigDecimal("1400000.00"),
                new BigDecimal("2100000.00"),
                7);
        NextPaymentMilestoneDto dto = new NextPaymentMilestoneDto(stage, summary);

        assertThat(dto.stage()).isNotNull();
        assertThat(dto.stage().stageNumber()).isEqualTo(4);
        assertThat(dto.stage().daysUntilDue()).isEqualTo(5);
        assertThat(dto.summary().stageCount()).isEqualTo(7);
    }

    @Test
    void stageMayBeNullWhenAllStagesAreTerminal() {
        NextPaymentMilestoneDto.Summary summary = new NextPaymentMilestoneDto.Summary(
                new BigDecimal("3500000.00"),
                new BigDecimal("3500000.00"),
                BigDecimal.ZERO,
                7);
        NextPaymentMilestoneDto dto = new NextPaymentMilestoneDto(null, summary);

        assertThat(dto.stage()).isNull();
        assertThat(dto.summary().totalOutstanding()).isEqualByComparingTo("0");
    }

    @Test
    void daysUntilDueAndDueDateMayBeNullWhenStageHasNoDueDate() {
        NextPaymentMilestoneDto.Stage stage = new NextPaymentMilestoneDto.Stage(
                3, "Foundation", null, null,
                "UPCOMING", new BigDecimal("250000"),
                new BigDecimal("10.0"), new BigDecimal("10.0"), 7);
        assertThat(stage.dueDate()).isNull();
        assertThat(stage.daysUntilDue()).isNull();
    }
}
