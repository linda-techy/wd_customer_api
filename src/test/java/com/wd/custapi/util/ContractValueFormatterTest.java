package com.wd.custapi.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractValueFormatterTest {

    @Test
    void nullAmountReturnsNull() {
        assertThat(ContractValueFormatter.formatINR(null)).isNull();
    }

    @Test
    void formatsCroresWithTwoDecimals() {
        // 1,20,00,000  -> "₹1.20 Cr"
        assertThat(ContractValueFormatter.formatINR(new BigDecimal("12000000")))
                .isEqualTo("₹1.20 Cr");
    }

    @Test
    void formatsLakhsWithoutDecimals() {
        // 45,00,000  -> "₹45 L"
        assertThat(ContractValueFormatter.formatINR(new BigDecimal("4500000")))
                .isEqualTo("₹45 L");
    }

    @Test
    void formatsBelowOneLakhWithIndianCommas() {
        // 5,500  -> "₹5,500"
        assertThat(ContractValueFormatter.formatINR(new BigDecimal("5500")))
                .isEqualTo("₹5,500");
    }

    @Test
    void formatsBoundaryCrore() {
        // 1,00,00,000  -> "₹1.00 Cr"
        assertThat(ContractValueFormatter.formatINR(new BigDecimal("10000000")))
                .isEqualTo("₹1.00 Cr");
    }

    @Test
    void formatsBoundaryLakh() {
        // 1,00,000  -> "₹1 L"  (rounded to no decimals for L)
        assertThat(ContractValueFormatter.formatINR(new BigDecimal("100000")))
                .isEqualTo("₹1 L");
    }

    @Test
    void rejectsNegative() {
        assertThatThrownBy(() -> ContractValueFormatter.formatINR(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formatsZeroAsRupeeZero() {
        assertThat(ContractValueFormatter.formatINR(BigDecimal.ZERO))
                .isEqualTo("\u20B90");
    }

    @Test
    void formatsSingleRupee() {
        assertThat(ContractValueFormatter.formatINR(new BigDecimal("1")))
                .isEqualTo("\u20B91");
    }

    @Test
    void roundsFractionalSubLakhToNearestRupee() {
        assertThat(ContractValueFormatter.formatINR(new BigDecimal("999.50")))
                .isEqualTo("\u20B91,000");
        assertThat(ContractValueFormatter.formatINR(new BigDecimal("999.49")))
                .isEqualTo("\u20B9999");
    }
}
