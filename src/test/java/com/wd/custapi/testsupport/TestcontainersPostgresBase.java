package com.wd.custapi.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base class for customer-api integration tests that need a real Postgres.
 *
 * <p>Starts a Postgres 16 container once per JVM via a static initializer and
 * reuses it across all test classes. Testcontainers' Ryuk sidecar tears it down
 * at JVM exit. Hibernate creates the schema from JPA entities, then Flyway
 * baselines at V998 so V999+ incremental migrations can apply on top.
 *
 * <p>JWT secret and audience are also injected here so subclasses don't have to
 * think about them. Override in a subclass's own {@code @DynamicPropertySource}
 * if a specific test needs different values.
 */
@SpringBootTest
public abstract class TestcontainersPostgresBase {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("custapi_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Entities are the source of truth for this project's schema. The Flyway
        // migrations (V999+) are incremental tweaks (indexes, ALTERs) whose cumulative
        // effect is already reflected in the current @Entity definitions.
        //
        // Spring Boot runs Flyway BEFORE Hibernate, so enabling Flyway here would run
        // V999 against an empty container ("relation site_reports does not exist") and
        // fail every @SpringBootTest. Conversely, running the migrations AFTER a full
        // Hibernate create would collide ("column already exists"), since the current
        // entities already contain every migration's columns. So for tests we let
        // Hibernate build the complete current schema from entities and disable Flyway —
        // the migrations add nothing the entities don't already define. (Production still
        // uses Flyway against the real shared DB.)
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("jwt.secret", () -> "test-secret-do-not-use-in-prod-0123456789abcdef0123456789abcdef");
        registry.add("jwt.access-token-expiration", () -> "3600000");
        registry.add("jwt.refresh-token-expiration", () -> "604800000");
        registry.add("jwt.aud.value", () -> "customer-api");
        registry.add("jwt.aud.enforce", () -> "false");
        // Disable rate limiting to prevent 429 in tests.
        registry.add("app.rate-limiting.enabled", () -> "false");
    }
}
