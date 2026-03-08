package com.wd.custapi.logging;

/**
 * Central constants for the logging system.
 * All logger names, MDC keys, and field names are defined here
 * to prevent typos and ensure consistency across the codebase.
 */
public final class LoggingConstants {

    private LoggingConstants() {}

    // ─── Named Logger Instances ───────────────────────────────────────────────
    /** HTTP access log — every request/response cycle */
    public static final String ACCESS_LOGGER = "ACCESS_LOGGER";

    /** Security events — login, JWT failures, unauthorized access */
    public static final String SECURITY_LOGGER = "SECURITY_LOGGER";

    /** Slow API detection — execution time > threshold */
    public static final String PERFORMANCE_LOGGER = "PERFORMANCE_LOGGER";

    // ─── MDC Keys ─────────────────────────────────────────────────────────────
    public static final String MDC_TRACE_ID  = "traceId";
    public static final String MDC_USER_ID   = "userId";
    public static final String MDC_USER_EMAIL = "userEmail";
    public static final String MDC_METHOD    = "httpMethod";
    public static final String MDC_PATH      = "httpPath";

    // ─── Performance Thresholds ───────────────────────────────────────────────
    /** API calls slower than this (ms) are logged as SLOW_API */
    public static final long SLOW_API_THRESHOLD_MS = 1000L;

    /** Service method calls slower than this are also logged */
    public static final long SLOW_SERVICE_THRESHOLD_MS = 2000L;

    // ─── Sensitive Field Names (for masking) ─────────────────────────────────
    public static final String[] SENSITIVE_FIELDS = {
        "password", "token", "authorization", "secret",
        "otp", "cvv", "credit_card", "creditcard", "pin"
    };

    // ─── Log Prefixes ─────────────────────────────────────────────────────────
    public static final String PREFIX_ACCESS      = "ACCESS";
    public static final String PREFIX_SECURITY    = "SECURITY";
    public static final String PREFIX_SLOW_API    = "SLOW_API";
    public static final String PREFIX_STARTUP     = "STARTUP";
}
