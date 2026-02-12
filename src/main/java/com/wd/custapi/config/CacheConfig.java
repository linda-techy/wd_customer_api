package com.wd.custapi.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache configuration for performance optimization.
 * Caches frequently accessed data like user project permissions.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure cache manager with specific caches.
     * userProjects cache: Stores project IDs for each user (TTL handled at usage level)
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("userProjects")
        ));
        return cacheManager;
    }
}
