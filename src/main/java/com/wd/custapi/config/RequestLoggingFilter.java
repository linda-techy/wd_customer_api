package com.wd.custapi.config;

import com.wd.custapi.logging.LoggingConstants;
import com.wd.custapi.logging.SensitiveDataMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * HTTP access logging filter — logs every request/response to access.log.
 *
 * Example output:
 *   ACCESS | POST /api/auth/login | 200 | 142ms | userId=null | ip=1.2.3.4 | ua=Mozilla/5.0 | traceId=REQ-a1b2c3d4
 *
 * Excludes: actuator health checks and static asset noise.
 * Masks: Authorization header (never logs tokens).
 */
@Component
@Order(2)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger ACCESS_LOG = LoggerFactory.getLogger(LoggingConstants.ACCESS_LOGGER);

    /** Paths that don't need access logging (health checks, favicon, etc.) */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator/health", "/favicon.ico", "/actuator/info"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String traceId   = MDC.get(LoggingConstants.MDC_TRACE_ID);
            String userId    = MDC.get(LoggingConstants.MDC_USER_ID);
            String method    = request.getMethod();
            String path      = request.getRequestURI();
            int    status    = response.getStatus();
            String clientIp  = resolveClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            // Mask user agent — never log it fully if it contains unusual chars
            String safeUa = userAgent != null
                    ? userAgent.substring(0, Math.min(userAgent.length(), 80))
                    : "unknown";

            ACCESS_LOG.info("{} | {} {} | {} | {}ms | userId={} | ip={} | ua={} | traceId={}",
                    LoggingConstants.PREFIX_ACCESS,
                    method, path,
                    status,
                    duration,
                    userId != null ? userId : "-",
                    clientIp,
                    safeUa,
                    traceId != null ? traceId : "-");
        }
    }

    /**
     * Resolve real client IP, handling reverse proxy headers safely.
     * Only trusts X-Forwarded-For if the request comes via Nginx.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Take only the first IP (rest are proxy hops)
            return SensitiveDataMasker.mask(forwarded.split(",")[0].trim());
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
