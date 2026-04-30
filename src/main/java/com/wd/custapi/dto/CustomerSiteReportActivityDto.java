package com.wd.custapi.dto;

/**
 * Customer-safe view of one row in {@code site_report_activities} (V84).
 *
 * <p>The Portal API owns this table. Customer side reads only — name,
 * manpower, optional equipment, optional notes. Internal audit columns
 * (created_by, etc.) intentionally not exposed.
 */
public record CustomerSiteReportActivityDto(
        Long id,
        String name,
        Integer manpower,
        String equipment,
        String notes
) {
}
