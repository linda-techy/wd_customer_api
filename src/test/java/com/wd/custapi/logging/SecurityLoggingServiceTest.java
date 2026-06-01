package com.wd.custapi.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link SecurityLoggingService}. No Spring context / DB — the service
 * has no injected collaborators (logs via a static SLF4J logger). We assert that every
 * public log method runs without throwing, that the trace-id MDC fallback works, and
 * exercise the email-masking branches directly via the private {@code maskEmail} helper.
 */
@ExtendWith(OutputCaptureExtension.class)
class SecurityLoggingServiceTest {

    private final SecurityLoggingService service = new SecurityLoggingService();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    // ── All public log methods run without throwing ───────────────────────────

    @Test
    void allPublicLogMethods_doNotThrow_noMdcTrace() {
        assertThatCode(() -> {
            service.logLoginSuccess("user@test.com", "1.2.3.4");
            service.logLoginFailure("user@test.com", "Bad credentials", "1.2.3.4");
            service.logLogout("user@test.com", "1.2.3.4");
            service.logJwtValidationFailure("expired", "1.2.3.4");
            service.logTokenRefreshFailure("revoked", "1.2.3.4");
            service.logUnauthorizedAccess("/api/x", "GET", "42", "1.2.3.4");
            service.logForbiddenAccess("/api/x", "POST", "42", "1.2.3.4");
            service.logAdminAction("admin@test.com", "DELETE_USER", "user:99");
            service.logRateLimitHit("user@test.com", "login", "1.2.3.4");
            service.logPasswordResetRequest("user@test.com", "1.2.3.4");
        }).doesNotThrowAnyException();
    }

    @Test
    void logUnauthorizedAccess_nullUserId_rendersAnonymous(CapturedOutput output) {
        service.logUnauthorizedAccess("/api/secret", "GET", null, "1.2.3.4");
        assertThat(output.getAll()).contains("userId=anonymous");
    }

    @Test
    void logForbiddenAccess_nonNullUserId_rendersUserId(CapturedOutput output) {
        service.logForbiddenAccess("/api/secret", "DELETE", "77", "1.2.3.4");
        assertThat(output.getAll()).contains("userId=77");
    }

    // ── traceId MDC branch ────────────────────────────────────────────────────

    @Test
    void traceId_presentInMdc_isUsed(CapturedOutput output) {
        MDC.put(LoggingConstants.MDC_TRACE_ID, "trace-xyz");
        service.logLoginFailure("user@test.com", "Bad credentials", "9.9.9.9");
        assertThat(output.getAll()).contains("traceId=trace-xyz");
    }

    @Test
    void traceId_absent_fallsBackToNoTrace(CapturedOutput output) {
        service.logLoginFailure("user@test.com", "Bad credentials", "9.9.9.9");
        assertThat(output.getAll()).contains("traceId=NO-TRACE");
    }

    // ── maskEmail branches (private helper) ───────────────────────────────────

    @Test
    void maskEmail_normalEmail_masksLocalPart() {
        assertThat(invokeMask("john.doe@example.com")).isEqualTo("j***@example.com");
    }

    @Test
    void maskEmail_null_returnsTripleStar() {
        assertThat(invokeMask(null)).isEqualTo("***");
    }

    @Test
    void maskEmail_shortString_returnsTripleStar() {
        assertThat(invokeMask("a@b")).isEqualTo("***"); // length < 4
    }

    @Test
    void logLoginSuccess_masksEmailInOutput(CapturedOutput output) {
        service.logLoginSuccess("alice@walldot.com", "1.2.3.4");
        assertThat(output.getAll()).contains("email=a***@walldot.com");
        assertThat(output.getAll()).doesNotContain("alice@walldot.com");
    }

    private String invokeMask(String email) {
        return (String) ReflectionTestUtils.invokeMethod(service, "maskEmail", email);
    }
}
