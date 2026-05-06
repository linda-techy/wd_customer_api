package com.wd.custapi.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the minimal Mon-Sat working-day calculator used by the customer
 * expected-handover endpoint. The customer-API does NOT consult the holiday
 * calendar (portal-API owns that table) — accuracy trade-off documented in
 * the calculator's Javadoc.
 */
class WorkingDayCalculatorTest {

    @Test
    void workingDaysBetween_returnsZeroForSameDay() {
        assertThat(WorkingDayCalculator.workingDaysBetween(
                LocalDate.of(2026, 5, 5), LocalDate.of(2026, 5, 5)))
                .isZero();
    }

    @Test
    void workingDaysBetween_skipsSundays() {
        // 2026-05-04 (Mon) -> 2026-05-11 (Mon) — range contains one Sunday (2026-05-10).
        // Counting Mon-Sat as working days, we expect 6 working days
        // (Mon Tue Wed Thu Fri Sat — Sunday excluded).
        assertThat(WorkingDayCalculator.workingDaysBetween(
                LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 11)))
                .isEqualTo(6);
    }

    @Test
    void workingDaysBetween_returnsNegativeWhenEndBeforeStart() {
        // 2026-05-08 (Fri) -> 2026-05-05 (Tue) — three working days backward
        // (Fri, Thu, Wed counted).
        assertThat(WorkingDayCalculator.workingDaysBetween(
                LocalDate.of(2026, 5, 8), LocalDate.of(2026, 5, 5)))
                .isEqualTo(-3);
    }

    @Test
    void workingDaysBetween_handlesYearBoundary() {
        // 2025-12-29 (Mon) -> 2026-01-05 (Mon) — range contains one Sunday (2026-01-04).
        // Mon Tue Wed Thu Fri Sat = 6 working days.
        assertThat(WorkingDayCalculator.workingDaysBetween(
                LocalDate.of(2025, 12, 29), LocalDate.of(2026, 1, 5)))
                .isEqualTo(6);
    }

    @Test
    void workingDaysBetween_handlesNullsGracefully() {
        assertThat(WorkingDayCalculator.workingDaysBetween(null, LocalDate.of(2026, 5, 5))).isZero();
        assertThat(WorkingDayCalculator.workingDaysBetween(LocalDate.of(2026, 5, 5), null)).isZero();
    }
}
