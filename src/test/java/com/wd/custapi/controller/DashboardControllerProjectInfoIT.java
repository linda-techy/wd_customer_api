package com.wd.custapi.controller;

import com.wd.custapi.service.JwtService;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests (TDD red step) for the extended GET /api/dashboard/projects/{uuid} endpoint.
 *
 * <p>These tests assert fields that are declared in {@link com.wd.custapi.dto.DashboardDto.ProjectDetails}
 * but are NOT yet populated by {@link com.wd.custapi.service.DashboardService#getProjectDetails}.
 * The tests will stay red until Task 6 implements the population logic.
 *
 * <p>Pattern copied from {@link FileDownloadIdorIntegrationTest}: JdbcTemplate inserts,
 * {@link JwtService#generateCustomerToken} for token minting, MockMvc for HTTP assertions.
 */
@AutoConfigureMockMvc
class DashboardControllerProjectInfoIT extends TestcontainersPostgresBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    // Customer A — owns project with budget set
    private String customerAToken;
    private String projectWithBudgetUuid;

    // Customer A — owns project with no budget
    private String projectNoBudgetUuid;

    // Customer B — does NOT own customer A's projects
    private String customerBToken;

    @BeforeEach
    void setUp() {
        // Clean slate (FK order: child tables before parent)
        // tasks and project_milestones have FKs to customer_projects (added in F2 Task 4)
        jdbc.update("DELETE FROM tasks");
        jdbc.update("DELETE FROM project_milestones");
        jdbc.update("DELETE FROM project_members");
        jdbc.update("DELETE FROM customer_projects");
        jdbc.update("DELETE FROM customer_users");

        // Ensure the CUSTOMER role exists
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");

        // ── Customer A ────────────────────────────────────────────────────────
        jdbc.update(
                "INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                        + "VALUES ('cust-a@test.com', 'x', 'CustomerA', 1, now(), true)");
        Long custAId = jdbc.queryForObject(
                "SELECT id FROM customer_users WHERE email = 'cust-a@test.com'", Long.class);

        // Project WITH budget = 12,000,000 and end_date set (used for estimatedCompletionDate fallback)
        String budgetUuid = UUID.randomUUID().toString();
        projectWithBudgetUuid = budgetUuid;
        Long projectWithBudgetId = jdbc.queryForObject(
                "INSERT INTO customer_projects "
                        + "(name, project_uuid, version, budget, end_date) "
                        + "VALUES ('Budget Project', ?::uuid, 0, 12000000.00, '2026-12-31') "
                        + "RETURNING id",
                Long.class, budgetUuid);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)",
                projectWithBudgetId, custAId);

        // Project WITHOUT budget (null)
        String noBudgetUuid = UUID.randomUUID().toString();
        projectNoBudgetUuid = noBudgetUuid;
        Long projectNoBudgetId = jdbc.queryForObject(
                "INSERT INTO customer_projects "
                        + "(name, project_uuid, version) "
                        + "VALUES ('No Budget Project', ?::uuid, 0) "
                        + "RETURNING id",
                Long.class, noBudgetUuid);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)",
                projectNoBudgetId, custAId);

        customerAToken = jwtService.generateCustomerToken("cust-a@test.com", new HashMap<>());

        // ── Customer B — no projects ──────────────────────────────────────────
        jdbc.update(
                "INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                        + "VALUES ('cust-b@test.com', 'x', 'CustomerB', 1, now(), true)");

        customerBToken = jwtService.generateCustomerToken("cust-b@test.com", new HashMap<>());
    }

    /**
     * RED: service does not yet populate contractValueDisplay.
     * Expected after Task 6: "₹1.20 Cr"
     */
    @Test
    void returnsContractValueDisplayWhenBudgetSet() throws Exception {
        mockMvc.perform(get("/api/dashboard/projects/" + projectWithBudgetUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractValueDisplay").value("\u20B91.20 Cr"));
    }

    /**
     * RED: service does not yet populate contractValueDisplay; null is expected when budget is null.
     * Jackson does not exclude nulls (no NON_NULL config), so the field is present with null value.
     */
    @Test
    void contractValueDisplayIsNullWhenBudgetNotSet() throws Exception {
        mockMvc.perform(get("/api/dashboard/projects/" + projectNoBudgetUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractValueDisplay").value(Matchers.nullValue()));
    }

    /**
     * GREEN immediately: Customer B does not own Customer A's project → 404.
     * The service throws "Project not found or access denied" which contains "not found",
     * so DashboardController.handleRuntimeException maps it to HTTP 404.
     */
    @Test
    void otherCustomerProjectReturns404() throws Exception {
        mockMvc.perform(get("/api/dashboard/projects/" + projectWithBudgetUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerBToken))
                .andExpect(status().isNotFound());
    }

    /**
     * GREEN immediately: no Authorization header → Spring Security rejects with 401 or 403.
     * (The existing SecurityConfig returns 403 for unauthenticated access to protected endpoints —
     * accept either to be resilient to security config changes.)
     */
    @Test
    void unauthenticatedReturns401() throws Exception {
        int status = mockMvc.perform(get("/api/dashboard/projects/" + projectWithBudgetUuid))
                .andReturn().getResponse().getStatus();
        // Spring Security typically returns 403 (not 401) when no token is present and
        // no WWW-Authenticate challenge is configured. Accept 401 or 403.
        org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
    }

    /**
     * RED: service does not yet populate estimatedCompletionDate.
     * Expected after Task 6: falls back to project end_date ("2026-12-31") when no Gantt data.
     */
    @Test
    void estimatedCompletionFallsBackToProjectEndDateWhenNoGantt() throws Exception {
        mockMvc.perform(get("/api/dashboard/projects/" + projectWithBudgetUuid)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedCompletionDate").value("2026-12-31"));
    }
}
