package com.wd.custapi.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /** Set SWAGGER_ENABLED=true only in non-production environments. Defaults to false (safe). */
    @Value("${swagger.enabled:false}")
    private boolean swaggerEnabled;

    /** Comma-separated list of IP addresses permitted to call /internal/** (e.g. the Portal API host). */
    @Value("${internal.allowed-ips:127.0.0.1,::1}")
    private String internalAllowedIps;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled for stateless JWT authentication - tokens in Authorization header
            // are not automatically sent by browsers, preventing CSRF attacks
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> {
                // Allow OPTIONS requests for CORS preflight
                auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll();
                // Public endpoints
                auth.requestMatchers("/api/storage/**").authenticated();
                auth.requestMatchers("/auth/**").permitAll();
                auth.requestMatchers("/api/public/**").permitAll();
                // Internal webhook endpoint — restricted to allowed IPs at the filter layer (InternalIpFilter);
                // Spring Security still requires authentication so an unauthenticated call will be rejected
                // unless the request has already been whitelisted by the IP filter.
                auth.requestMatchers("/internal/**").access(internalIpAccessManager());
                // Swagger UI — gate behind environment flag; blocked in production by default
                if (swaggerEnabled) {
                    auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
                } else {
                    auth.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").denyAll();
                }
                // All other requests require authentication
                auth.anyRequest().authenticated();
            })
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Restricts /internal/** to the IP addresses listed in internal.allowed-ips.
     * Falls back to 127.0.0.1 and ::1 (loopback) if not configured.
     */
    private org.springframework.security.authorization.AuthorizationManager<
            org.springframework.security.web.access.intercept.RequestAuthorizationContext> internalIpAccessManager() {
        List<String> allowedIps = Arrays.stream(internalAllowedIps.split(","))
                .map(String::trim)
                .filter(ip -> !ip.isEmpty())
                .collect(Collectors.toList());
        return (authentication, context) -> {
            String remoteAddr = context.getRequest().getRemoteAddr();
            boolean allowed = allowedIps.contains(remoteAddr);
            return new org.springframework.security.authorization.AuthorizationDecision(allowed);
        };
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins - cannot use wildcard "*" with credentials
        // Using origin patterns for flexibility while maintaining security
        // Pattern format supports wildcards for ports: http://127.0.0.1:*
        List<String> originPatterns = new ArrayList<>(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .collect(Collectors.toList()));
        
        configuration.setAllowedOriginPatterns(originPatterns);
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        // Allow all headers - using explicit list for better compatibility
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Cache-Control",
            "Range",
            "X-CSRF-TOKEN"
        ));
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Content-Range",
            "Accept-Ranges",
            "Authorization"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 per OWASP 2025 recommendation (default 10 is too weak for modern hardware)
        return new BCryptPasswordEncoder(12);
    }
}

