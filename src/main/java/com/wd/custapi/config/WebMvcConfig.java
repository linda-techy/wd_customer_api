package com.wd.custapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration — registers interceptors and handler mappings.
 *
 * The {@link AuthRateLimitInterceptor} is scoped to {@code /auth/**} only.
 * All other request paths bypass rate limiting.
 * Set {@code app.rate-limiting.enabled=false} to disable (e.g. in tests).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimiterConfig rateLimiterConfig;

    @Value("${app.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    public WebMvcConfig(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitingEnabled) {
            registry.addInterceptor(new AuthRateLimitInterceptor(rateLimiterConfig))
                    .addPathPatterns("/auth/**");
        }
    }
}
