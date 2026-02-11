package com.wd.custapi.config;

import org.springframework.context.annotation.Configuration;

/**
 * Application configuration for the customer API.
 * This configuration provides constants and settings for the customer application.
 */
@Configuration
public class AppConfig {
    
    // Application-specific constants
    public static final String CONTEXT_PATH = "/api";
    public static final String DATABASE_SCHEMA = "public";
    
    // Database table constants for customer users
    // Note: These tables are auto-created by Hibernate (ddl-auto=update).
    // Portal API's Flyway migration V1_46 dropped customer_permissions and
    // customer_role_permissions, but Hibernate recreates them on startup since
    // the Permission entity and Role's @ManyToMany JoinTable reference them.
    public static final String USER_TABLE = "customer_users";
    public static final String ROLE_TABLE = "customer_roles";
    public static final String PERMISSION_TABLE = "customer_permissions";
    public static final String REFRESH_TOKEN_TABLE = "customer_refresh_tokens";
}

