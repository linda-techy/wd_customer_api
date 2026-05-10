package com.wd.custapi.dto;

/**
 * Event types published by the Portal API and consumed by the Customer API
 * via the internal webhook endpoint POST /internal/portal-events.
 */
public enum PortalEventType {
    INVOICE_ISSUED,
    INVOICE_PAID,
    PHASE_UPDATED,
    MILESTONE_REACHED,
    SITE_REPORT_SUBMITTED,
    DOCUMENT_UPLOADED,
    PAYMENT_RECORDED,
    DELAY_REPORTED,
    /**
     * S3 PR3 — customer's expected handover date moved by more than 3 working
     * days from the last alerted value (or, on first alert, from the approved
     * baseline). Metadata: oldDate, newDate, shiftWorkingDays (signed),
     * direction ("earlier"|"later").
     */
    HANDOVER_SHIFT,
    /**
     * S6 PR2 — daily reminder for an upcoming (T-3), due-today, or overdue
     * payment stage. Metadata: reminderKind ("T_MINUS_3" | "DUE_TODAY" |
     * "OVERDUE"), stageId, stageNumber, stageName, dueDate (ISO-8601),
     * netPayableAmount (plain BigDecimal string).
     */
    PAYMENT_MILESTONE_DUE
}
