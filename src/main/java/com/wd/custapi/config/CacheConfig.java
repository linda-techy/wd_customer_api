package com.wd.custapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine for bounded in-memory caching.
 * All caches have a maximum size and TTL to prevent unbounded memory growth.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * userProjects cache: Stores project lookup results per user.
     * - Max 5,000 entries (covers ~5k concurrent user sessions)
     * - Expires 5 minutes after last write
     * - Automatically evicts least-recently-used entries when full
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("userProjects");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()); // enables cache hit/miss metrics
        return manager;
    }
}
