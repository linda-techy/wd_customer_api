package com.wd.custapi.controller;

import com.wd.custapi.service.JwtService;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests (TDD red step) for the not-yet-implemented
 * GET /api/customer/projects/{uuid}/timeline endpoint.
 *
 * <p>Until Task 18 adds {@code CustomerTimelineController}, every request to
 * this path will fall through to Spring's default "no handler" response
 * (404 from {@code NoHandlerFoundException}), or 401/403 for unauthenticated
 * requests blocked before the dispatcher. That is the intended red state.
 *
 * <p>Pattern mirrors {@link CustomerTeamControllerIT}:
 * JdbcTemplate inserts, {@link JwtService#generateCustomerToken} for JWT
 * minting, MockMvc for HTTP assertions.
 *
 * <p>Docker/Testcontainers: these tests require a live Postgres container.
 * They are Docker-deferred in CI environments without Docker (they will be
 * skipped by {@link TestcontainersPostgresBase}).
 */
@AutoConfigureMockMvc
class CustomerTimelineControllerIT extends TestcontainersPostgresBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    // Customer A — owns the project
    private String customerAToken;
    private String projectUuid;

    // Customer B — does NOT own Customer A's project
    private String customerBToken;

    @BeforeEach
    void setUp() {
        // ── Clean slate (FK order matters) ───────────────────────────────────
        jdbc.update("DELETE FROM tasks");
        jdbc.update("DELETE FROM project_milestones");
        jdbc.update("DELETE FROM project_members");
        jdbc.update("DELETE FROM customer_projects");
        jdbc.update("DELETE FROM customer_users");

        // Ensure the CUSTOMER role exists
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");

        // ── Customer A ───────────────────────────────────────────────────────
        jdbc.update(
                "INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                        + "VALUES ('cust-timeline-a@test.com', 'x', 'CustomerA', 1, now(), true)");
        Long custAId = jdbc.queryForObject(
                "SELECT id FROM customer_users WHERE email = 'cust-timeline-a@test.com'", Long.class);

        // Customer A's project
        String uuid = UUID.randomUUID().toString();
        projectUuid = uuid;
        Long projectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) "
                        + "VALUES ('Timeline Test Project', ?::uuid, 0) RETURNING id",
                Long.class, uuid);

        // Link customer A as the project member
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)",
                projectId, custAId);

        // ── Insert a customer-visible task in the UPCOMING bucket ─────────────
        // start_date far in the future so it always lands in UPCOMING
        jdbc.update("""
                INSERT INTO tasks
                    (title, status, priority, due_date, start_date, end_date,
                     progress_percent, customer_visible, project_id, created_at, updated_at)
                VALUES
                    ('Upcoming Visible Task', 'IN_PROGRESS', 'MEDIUM',
                     '2099-12-31', '2099-01-01', '2099-12-31',
                     10, TRUE, ?, now(), now())
                """, projectId);

        // ── Insert a customer_visible=FALSE task — must be excluded ──────────
        jdbc.update("""
                INSERT INTO tasks
                    (title, status, priority, due_date, start_date, end_date,
                     progress_percent, customer_visible, project_id, created_at, updated_at)
                VALUES
                    ('Hidden Task', 'IN_PROGRESS', 'LOW',
                     '2099-12-31', '2099-01-01', '2099-12-31',
                     20, FALSE, ?, now(), now())
                """, projectId);

        // ── Insert a milestone for progress rollup ────────────────────────────
        jdbc.update("""
                INSERT INTO project_milestones
                    (project_id, name, status, completion_percentage, weight_percentage)
                VALUES
                    (?, 'M1', 'IN_PROGRESS', 50.00, 1.00)
                """, projectId);

        customerAToken = jwtService.generateCustomerToken("cust-timeline-a@test.com", new HashMap<>());

        // ── Customer B — no projects ──────────────────────────────────────────
        jdbc.update(
                "INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                        + "VALUES ('cust-timeline-b@test.com', 'x', 'CustomerB', 1, now(), true)");
        customerBToken = jwtService.generateCustomerToken("cust-timeline-b@test.com", new HashMap<>());
    }

    /**
     * RED: no handler registered yet.
     * After Task 18: expects 200 with bucketed items for the owned project.
     * The upcoming bucket should return the visible task, not the hidden one.
     */
    @Test
    void returnsTimelineBucketForOwnedProject() throws Exception {
        mockMvc.perform(get("/api/customer/projects/" + projectUuid + "/timeline")
                        .param("bucket", "upcoming")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bucket").value("upcoming"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Upcoming Visible Task"))
                .andExpect(jsonPath("$.projectProgressPercent").isNumber());
    }

    /**
     * RED: no handler yet.
     * After Task 18: customer_visible=FALSE task must be excluded from all buckets.
     */
    @Test
    void excludesNonCustomerVisibleTasks() throws Exception {
        // Upcoming bucket should have exactly 1 task (the visible one); hidden task excluded
        mockMvc.perform(get("/api/customer/projects/" + projectUuid + "/timeline")
                        .param("bucket", "upcoming")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /**
     * RED: no handler yet.
     * After Task 18: Customer B does not own Customer A's project → 404.
     */
    @Test
    void returns404ForNonOwnedProject() throws Exception {
        mockMvc.perform(get("/api/customer/projects/" + projectUuid + "/timeline")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerBToken))
                .andExpect(status().isNotFound());
    }

    /**
     * No Authorization header — Spring Security rejects before the dispatcher.
     * Accept 401 or 403 to be resilient to security config changes.
     */
    @Test
    void returns401Unauthenticated() throws Exception {
        int statusCode = mockMvc.perform(get("/api/customer/projects/" + projectUuid + "/timeline"))
                .andReturn().getResponse().getStatus();
        assertThat(statusCode).isIn(401, 403);
    }

    /**
     * RED: no handler yet.
     * After Task 18: /summary endpoint returns task counts and projectProgressPercent.
     */
    @Test
    void summaryReturnsCountsAndProgress() throws Exception {
        mockMvc.perform(get("/api/customer/projects/" + projectUuid + "/timeline/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekCount").isNumber())
                .andExpect(jsonPath("$.upcomingCount").value(1))
                .andExpect(jsonPath("$.completedCount").isNumber())
                .andExpect(jsonPath("$.projectProgressPercent").isNumber());
    }

    /**
     * RED: no handler yet.
     * After Task 18: projectProgressPercent in the list response must be
     * non-null and within [0, 100].
     */
    @Test
    void projectProgressPercentIsWithinBounds() throws Exception {
        String json = mockMvc.perform(get("/api/customer/projects/" + projectUuid + "/timeline")
                        .param("bucket", "upcoming")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = mapper.readValue(json, java.util.Map.class);

        Object pct = body.get("projectProgressPercent");
        assertThat(pct).isNotNull();
        int pctInt = ((Number) pct).intValue();
        assertThat(pctInt).isBetween(0, 100);
    }
}
