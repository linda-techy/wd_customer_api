package com.wd.custapi.controller;

import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.service.CrAccessGuard;
import com.wd.custapi.service.PortalApiClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Customer-facing endpoints for change-request OTP flow.
 *
 * Both endpoints are session-authenticated (JWT). They translate the customer
 * action into an HMAC-signed call to portal-API's /internal/cr-* endpoints
 * via {@link PortalApiClient}.
 */
@RestController
@RequestMapping("/api/customer/cr")
public class CustomerCrApprovalController {

    private static final Logger log = LoggerFactory.getLogger(CustomerCrApprovalController.class);

    private final PortalApiClient portalApiClient;
    private final CustomerUserRepository customerUserRepository;
    private final CrAccessGuard crAccessGuard;

    public CustomerCrApprovalController(PortalApiClient portalApiClient,
                                         CustomerUserRepository customerUserRepository,
                                         CrAccessGuard crAccessGuard) {
        this.portalApiClient = portalApiClient;
        this.customerUserRepository = customerUserRepository;
        this.crAccessGuard = crAccessGuard;
    }

    public record ApproveBody(@NotBlank @Pattern(regexp = "\\d{6}") String otpCode) {}

    @PostMapping("/{crId}/request-otp")
    public ResponseEntity<Map<String, Object>> requestOtp(@PathVariable Long crId,
                                                           Authentication auth) {
        Long customerUserId = resolveCustomerUserId(auth);
        // Ownership gate: assert this customer owns the project of this CR
        // BEFORE forwarding to portal. Closes DoS-against-rate-limits and
        // audit-pollution holes against arbitrary crId enumeration.
        crAccessGuard.assertCustomerCanAccessCr(customerUserId, auth.getName(), crId);

        try {
            Map<String, Object> result = portalApiClient.requestCrOtp(crId, customerUserId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
        } catch (PortalApiClient.RateLimitedException e) {
            log.warn("CR {} customer {} rate-limited", crId, customerUserId);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "error", "RATE_LIMITED",
                "retryAfterSeconds", e.getRetryAfterSeconds()));
        }
    }

    @PostMapping("/{crId}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long crId,
                                                        @RequestBody ApproveBody body,
                                                        HttpServletRequest request,
                                                        Authentication auth) {
        Long customerUserId = resolveCustomerUserId(auth);
        // Same ownership gate as request-otp.
        crAccessGuard.assertCustomerCanAccessCr(customerUserId, auth.getName(), crId);

        String customerIp = extractClientIp(request);

        Map<String, Object> result = portalApiClient.approveCr(
            crId, customerUserId, body.otpCode(), customerIp);

        Object status = result.get("status");
        if ("VERIFIED".equals(status)) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(result);
    }

    private Long resolveCustomerUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        CustomerUser u = customerUserRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                "Unknown customer: " + auth.getName()));
        return u.getId();
    }

    /** Take the first hop of X-Forwarded-For if present; otherwise remote addr. */
    static String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
