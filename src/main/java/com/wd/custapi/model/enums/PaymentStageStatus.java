package com.wd.custapi.model.enums;

/**
 * Mirrors the portal-API source-of-truth enum for the shared
 * {@code payment_stages.status} column. The customer-API reads these rows
 * via JPA, so every value the portal can write MUST appear here or
 * Hibernate throws {@code IllegalArgumentException: No enum constant} on
 * row materialisation.
 *
 * <p>The original customer-API set ({@code PENDING}, {@code PARTIALLY_PAID})
 * is preserved for backward compatibility with any existing read paths
 * that compared against those names — this change is purely additive.
 */
public enum PaymentStageStatus {
    UPCOMING,
    DUE,
    PENDING,
    INVOICED,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    ON_HOLD
}
