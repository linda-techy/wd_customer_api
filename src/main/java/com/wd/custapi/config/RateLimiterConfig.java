package com.wd.custapi.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter using Bucket4j token-bucket algorithm.
 * Protects authentication endpoints from brute force and enumeration attacks.
 *
 * Limits (per IP, per minute):
 *   /auth/login             — 5 attempts
 *   /auth/refresh-token     — 20 attempts
 *   /auth/forgot-password   — 5 attempts
 *   /auth/reset-password    — 5 attempts
 *
 * For distributed deployments, replace the ConcurrentHashMap cache with
 * a Redis-backed Bucket4j ProxyManager.
 */
@Configuration
public class RateLimiterConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Return (or create) a rate-limit bucket for the given key.
     * Each unique key gets its own independent token bucket.
     */
    public Bucket resolveBucket(String key, int capacity, Duration refillDuration) {
        return buckets.computeIfAbsent(key, k ->
            Bucket.builder()
                .addLimit(Bandwidth.builder()
                    .capacity(capacity)
                    .refillIntervally(capacity, refillDuration)
                    .build())
                .build()
        );
    }
}
