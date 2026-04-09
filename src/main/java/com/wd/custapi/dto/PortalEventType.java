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
    PAYMENT_RECORDED
}
