package com.wd.custapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration — registers interceptors and handler mappings.
 *
 * The {@link AuthRateLimitInterceptor} is scoped to {@code /auth/**} only.
 * All other request paths bypass rate limiting.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimiterConfig rateLimiterConfig;

    public WebMvcConfig(RateLimiterConfig rateLimiterConfig) {
        this.rateLimiterConfig = rateLimiterConfig;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthRateLimitInterceptor(rateLimiterConfig))
                .addPathPatterns("/auth/**");
    }
}
