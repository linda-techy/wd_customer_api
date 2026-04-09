package com.wd.custapi.config;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * IP-based rate limiter for customer authentication endpoints.
 *
 * Limits (per IP, per 1-minute window):
 *   POST /auth/login            — 5 attempts  (brute force protection)
 *   POST /auth/refresh-token    — 20 attempts (normal refresh cycle)
 *   POST /auth/forgot-password  — 5 attempts  (enumeration prevention)
 *   POST /auth/reset-password   — 5 attempts  (token exhaustion prevention)
 *
 * Returns HTTP 429 with a Retry-After header on limit breach.
 * Token bucket state is in-memory — safe for single-instance; upgrade to Redis for scale.
 */
public class AuthRateLimitInterceptor implements HandlerInterceptor {

    private static final int LOGIN_CAPACITY           = 5;
    private static final int REFRESH_CAPACITY         = 20;
    private static final int PASSWORD_RESET_CAPACITY  = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimiterConfig rateLimiterConfig;

    public AuthRateLimitInterceptor(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String uri = request.getRequestURI();
        int capacity = resolveCapacity(uri);
        if (capacity == 0) {
            return true; // Not a rate-limited endpoint
        }

        String ip = resolveClientIp(request);
        String bucketKey = "auth:" + uri + ":" + ip;
        Bucket bucket = rateLimiterConfig.resolveBucket(bucketKey, capacity, WINDOW);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"success\":false,\"message\":\"Too many requests. Retry after %d seconds.\"}",
            retryAfterSeconds
        ));
        return false;
    }

    private int resolveCapacity(String uri) {
        if (uri.endsWith("/auth/login")) {
            return LOGIN_CAPACITY;
        }
        if (uri.endsWith("/auth/refresh-token")) {
            return REFRESH_CAPACITY;
        }
        if (uri.endsWith("/auth/forgot-password") || uri.endsWith("/auth/reset-password")) {
            return PASSWORD_RESET_CAPACITY;
        }
        return 0;
    }

    /**
     * Extracts real client IP, respecting X-Forwarded-For from reverse proxies (Nginx/Traefik).
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
