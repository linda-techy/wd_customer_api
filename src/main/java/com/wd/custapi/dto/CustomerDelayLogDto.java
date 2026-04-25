package com.wd.custapi.dto;

import com.wd.custapi.model.DelayLog;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Customer-safe delay log projection.
 *
 * Deliberately omits the raw internal fields:
 *   - reason_text         (free-text, may contain internal notes/blame)
 *   - responsible_party   (free-text, vendor/subcontractor names leak here)
 *   - impact_description  (free-text, internal)
 *
 * The customer sees a curated `customerSummary` plus a bounded
 * `impactOnHandover` enum, filtered server-side to rows where
 * `customer_visible = true`.
 */
public record CustomerDelayLogDto(
        Long id,
        String delayType,          // bounded enum, safe
        String reasonCategory,     // bounded enum, safe
        LocalDate fromDate,
        LocalDate toDate,
        Integer durationDays,
        String customerSummary,    // curated by portal user
        String impactOnHandover,   // NONE | MINOR | MATERIAL
        boolean isOpen,
        long impactDays
) {
    public static CustomerDelayLogDto from(DelayLog d) {
        boolean open = d.getToDate() == null;
        LocalDate end = open ? LocalDate.now() : d.getToDate();
        long days = ChronoUnit.DAYS.between(d.getFromDate(), end);
        return new CustomerDelayLogDto(
                d.getId(),
                d.getDelayType(),
                d.getReasonCategory(),
                d.getFromDate(),
                d.getToDate(),
                d.getDurationDays(),
                d.getCustomerSummary(),
                d.getImpactOnHandover(),
                open,
                days
        );
    }
}
