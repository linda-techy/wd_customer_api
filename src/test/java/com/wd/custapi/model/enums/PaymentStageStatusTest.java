package com.wd.custapi.model.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStageStatusTest {

    @Test
    void enumContainsAllPortalWrittenValues() {
        // The portal-API enum is the source of truth for payment_stages.status.
        // Customer-API materialises these rows via JPA — every value the portal
        // writes must be present here or Hibernate throws on read.
        assertThat(PaymentStageStatus.values())
                .extracting(Enum::name)
                .contains("UPCOMING", "DUE", "INVOICED", "PAID", "OVERDUE", "ON_HOLD");
    }

    @Test
    void preExistingValuesAreStillPresentForBackwardCompatibility() {
        // PENDING and PARTIALLY_PAID may be referenced by existing customer-api code paths.
        // Keep them so this enum change is purely additive.
        assertThat(PaymentStageStatus.values())
                .extracting(Enum::name)
                .contains("PENDING", "PARTIALLY_PAID");
    }
}
