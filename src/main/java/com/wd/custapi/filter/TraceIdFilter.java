package com.wd.custapi.filter;

import com.wd.custapi.logging.LoggingConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * First filter in the chain — runs before everything else.
 *
 * Responsibilities:
 *  1. Generate or propagate a unique traceId (from X-Trace-ID header or new REQ-{uuid})
 *  2. Store traceId + userId in MDC so every subsequent log line includes them
 *  3. Return traceId in response header for client-side correlation
 *  4. Clean MDC after request completes (prevents memory leaks in thread pools)
 *
 * Example MDC values visible in every log line:
 *   [traceId=REQ-a1b2c3d4] [userId=42]
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER_IN  = "X-Trace-ID";
    private static final String TRACE_HEADER_OUT = "X-Trace-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Resolve traceId — prefer client header for distributed tracing
        String traceId = request.getHeader(TRACE_HEADER_IN);
        if (traceId == null || traceId.isBlank()) {
            // Generate short, human-readable trace ID
            traceId = "REQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }

        // 2. Populate MDC — all log lines from this point will include traceId
        MDC.put(LoggingConstants.MDC_TRACE_ID, traceId);
        MDC.put(LoggingConstants.MDC_METHOD, request.getMethod());
        MDC.put(LoggingConstants.MDC_PATH, request.getRequestURI());

        // 3. Respond with traceId so clients can correlate errors
        response.setHeader(TRACE_HEADER_OUT, traceId);

        try {
            filterChain.doFilter(request, response);

            // 4. Populate userId AFTER security filter has run (so principal is available)
            populateUserIdMdc();
        } finally {
            // 5. Always clean MDC — prevents thread pool contamination
            MDC.remove(LoggingConstants.MDC_TRACE_ID);
            MDC.remove(LoggingConstants.MDC_USER_ID);
            MDC.remove(LoggingConstants.MDC_USER_EMAIL);
            MDC.remove(LoggingConstants.MDC_METHOD);
            MDC.remove(LoggingConstants.MDC_PATH);
        }
    }

    private void populateUserIdMdc() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                if (username != null && !username.isBlank()) {
                    MDC.put(LoggingConstants.MDC_USER_EMAIL, username);
                }
            }
        } catch (Exception ignored) {
            // Never let MDC population fail a request
        }
    }
}
