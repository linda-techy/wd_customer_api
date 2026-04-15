package com.wd.custapi.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared base class for customer-api integration tests that need a real Postgres.
 *
 * <p>Starts a Postgres 16 container once per JVM (shared across test classes via
 * the static field), runs Flyway migrations on Spring Boot startup, and points
 * the application at the container via {@link DynamicPropertySource}.
 *
 * <p>JWT secret and audience are also injected here so subclasses don't have to
 * think about them. Override in a subclass's own {@code @DynamicPropertySource}
 * if a specific test needs different values.
 */
@SpringBootTest
@Testcontainers
public abstract class TestcontainersPostgresBase {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("custapi_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // This project's schema was bootstrapped by Hibernate DDL before Flyway was introduced.
        // The Flyway migrations (V999+) are incremental-only (ALTER TABLE, CREATE INDEX).
        // On a fresh container we let Hibernate create the full schema from JPA entities, then
        // mark V998 as the Flyway baseline so all incremental migrations (V999+) run on top.
        // This mirrors the documented production bootstrap procedure in V1__baseline_schema.sql.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "998");
        registry.add("spring.flyway.out-of-order", () -> "false");
        registry.add("jwt.secret", () -> "test-secret-do-not-use-in-prod-0123456789abcdef0123456789abcdef");
        registry.add("jwt.access-token-expiration", () -> "3600000");
        registry.add("jwt.refresh-token-expiration", () -> "604800000");
        registry.add("jwt.aud.value", () -> "customer-api");
        registry.add("jwt.aud.enforce", () -> "false");
    }
}
