package com.wd.custapi.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Service for logging security-related events to security.log.
 *
 * All security events are structured with:
 *   SECURITY | EVENT_TYPE | detail | traceId=... | ip=...
 *
 * Usage example:
 *   securityLoggingService.logLoginFailure("user@test.com", "Bad credentials", "1.2.3.4");
 *
 * IMPORTANT: This service must NOT log passwords, tokens, OTPs, or full authorization headers.
 */
@Service
public class SecurityLoggingService {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger(LoggingConstants.SECURITY_LOGGER);

    // ─── Authentication Events ────────────────────────────────────────────────

    public void logLoginSuccess(String email, String ip) {
        SECURITY_LOG.info("{} | LOGIN_SUCCESS | email={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                maskEmail(email), ip, getTraceId());
    }

    public void logLoginFailure(String email, String reason, String ip) {
        SECURITY_LOG.warn("{} | LOGIN_FAILURE | email={} | reason={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                maskEmail(email), reason, ip, getTraceId());
    }

    public void logLogout(String email, String ip) {
        SECURITY_LOG.info("{} | LOGOUT | email={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                maskEmail(email), ip, getTraceId());
    }

    // ─── JWT / Token Events ───────────────────────────────────────────────────

    public void logJwtValidationFailure(String reason, String ip) {
        SECURITY_LOG.warn("{} | JWT_VALIDATION_FAILURE | reason={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                reason, ip, getTraceId());
    }

    public void logTokenRefreshFailure(String reason, String ip) {
        SECURITY_LOG.warn("{} | TOKEN_REFRESH_FAILURE | reason={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                reason, ip, getTraceId());
    }

    // ─── Authorization Events ─────────────────────────────────────────────────

    public void logUnauthorizedAccess(String path, String method, String userId, String ip) {
        SECURITY_LOG.warn("{} | UNAUTHORIZED_ACCESS | {} {} | userId={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                method, path,
                userId != null ? userId : "anonymous",
                ip, getTraceId());
    }

    public void logForbiddenAccess(String path, String method, String userId, String ip) {
        SECURITY_LOG.warn("{} | FORBIDDEN_ACCESS | {} {} | userId={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                method, path,
                userId != null ? userId : "anonymous",
                ip, getTraceId());
    }

    // ─── Admin / Privileged Actions ───────────────────────────────────────────

    public void logAdminAction(String adminEmail, String action, String targetResource) {
        SECURITY_LOG.info("{} | ADMIN_ACTION | admin={} | action={} | target={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                maskEmail(adminEmail), action, targetResource, getTraceId());
    }

    // ─── Rate Limit Events ────────────────────────────────────────────────────

    public void logRateLimitHit(String email, String operation, String ip) {
        SECURITY_LOG.warn("{} | RATE_LIMIT_HIT | email={} | operation={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                maskEmail(email), operation, ip, getTraceId());
    }

    public void logPasswordResetRequest(String email, String ip) {
        SECURITY_LOG.info("{} | PASSWORD_RESET_REQUEST | email={} | ip={} | traceId={}",
                LoggingConstants.PREFIX_SECURITY,
                maskEmail(email), ip, getTraceId());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Partially mask email: u***@domain.com to avoid full PII in logs */
    private String maskEmail(String email) {
        if (email == null || email.length() < 4) return "***";
        return email.replaceAll("(.).+(@.+)", "$1***$2");
    }

    private String getTraceId() {
        String traceId = MDC.get(LoggingConstants.MDC_TRACE_ID);
        return traceId != null ? traceId : "NO-TRACE";
    }
}
