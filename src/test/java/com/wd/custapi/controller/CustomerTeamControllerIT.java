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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests (TDD red step) for the not-yet-implemented
 * GET /api/dashboard/projects/{uuid}/team endpoint.
 *
 * <p>Until Task 10 adds {@code CustomerTeamController}, every request to
 * this path will fall through to Spring's default "no handler" response
 * (404 from {@code NoHandlerFoundException} or from
 * {@code DefaultHandlerExceptionResolver}), or 401/403 for unauthenticated
 * requests that are blocked before the dispatcher reaches that point.
 * That is the intended red state.
 *
 * <p>Pattern mirrors {@link DashboardControllerProjectInfoIT}:
 * JdbcTemplate inserts, {@link JwtService#generateCustomerToken} for JWT
 * minting, MockMvc for HTTP assertions.
 */
@AutoConfigureMockMvc
class CustomerTeamControllerIT extends TestcontainersPostgresBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    // Customer A — the project owner
    private String customerAToken;
    private String projectUuid;

    // Customer B — does NOT own Customer A's project
    private String customerBToken;

    @BeforeEach
    void setUp() {
        // ── Clean slate (FK order: child tables before parent) ───────────────
        // tasks and project_milestones have FKs to customer_projects (added in F2 Task 4)
        jdbc.update("DELETE FROM tasks");
        jdbc.update("DELETE FROM project_milestones");
        jdbc.update("DELETE FROM project_members");
        jdbc.update("DELETE FROM customer_projects");
        jdbc.update("DELETE FROM customer_users");

        // portal_users is not a JPA entity in this service — create it if absent
        // so PortalUserLookupJpaImpl can query it during the green run (Task 10).
        jdbc.update("""
                CREATE TABLE IF NOT EXISTS portal_users (
                    id         BIGSERIAL PRIMARY KEY,
                    first_name VARCHAR(100),
                    last_name  VARCHAR(100),
                    email      VARCHAR(255),
                    phone      VARCHAR(32)
                )
                """);
        jdbc.update("DELETE FROM portal_users");

        // Ensure the CUSTOMER role exists
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");

        // ── Customer A ───────────────────────────────────────────────────────
        jdbc.update(
                "INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                        + "VALUES ('cust-team-a@test.com', 'x', 'CustomerA', 1, now(), true)");
        Long custAId = jdbc.queryForObject(
                "SELECT id FROM customer_users WHERE email = 'cust-team-a@test.com'", Long.class);

        // Customer A's project
        String uuid = UUID.randomUUID().toString();
        projectUuid = uuid;
        Long projectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) "
                        + "VALUES ('Team Test Project', ?::uuid, 0) RETURNING id",
                Long.class, uuid);

        // Link customer A as the customer member of the project
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)",
                projectId, custAId);

        // ── Portal users: PM (share=TRUE) and Architect (share=FALSE) ────────
        Long pmPortalUserId = jdbc.queryForObject(
                "INSERT INTO portal_users (first_name, last_name, email, phone) "
                        + "VALUES ('PM', 'User', 'pm@x.com', '+91-1111') RETURNING id",
                Long.class);

        Long archPortalUserId = jdbc.queryForObject(
                "INSERT INTO portal_users (first_name, last_name, email, phone) "
                        + "VALUES ('Arch', 'User', 'ar@x.com', '+91-2222') RETURNING id",
                Long.class);

        // Staff members linked to the project
        jdbc.update(
                "INSERT INTO project_members (project_id, portal_user_id, role_in_project, share_with_customer) "
                        + "VALUES (?, ?, 'PROJECT_MANAGER', TRUE)",
                projectId, pmPortalUserId);

        jdbc.update(
                "INSERT INTO project_members (project_id, portal_user_id, role_in_project, share_with_customer) "
                        + "VALUES (?, ?, 'ARCHITECT', FALSE)",
                projectId, archPortalUserId);

        customerAToken = jwtService.generateCustomerToken("cust-team-a@test.com", new HashMap<>());

        // ── Customer B — no projects ──────────────────────────────────────────
        jdbc.update(
                "INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                        + "VALUES ('cust-team-b@test.com', 'x', 'CustomerB', 1, now(), true)");

        customerBToken = jwtService.generateCustomerToken("cust-team-b@test.com", new HashMap<>());
    }

    /**
     * RED: no handler registered yet.
     * After Task 10: expects 200 + 2 team members with phone/email gated by
     * share_with_customer.
     * <ul>
     *   <li>PROJECT_MANAGER has share_with_customer=TRUE → phone and email present</li>
     *   <li>ARCHITECT has share_with_customer=FALSE → phone and email absent (null fields
     *       or fields not serialised)</li>
     * </ul>
     * JsonPath array-filter syntax {@code $[?(@.role=='X')].field} is used here;
     * if the Jayway version in this project doesn't support it the test will fail
     * with a JsonPath parse error rather than an assertion error — that's still red
     * and should be fixed in Task 10 alongside the controller.
     */
    @Test
    void returnsTeamWithVisibilityGatedFields() throws Exception {
        String json = mockMvc.perform(get("/api/dashboard/projects/" + projectUuid + "/team")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn().getResponse().getContentAsString();

        // Materialise as list and assert visibility gating via Java — avoids any
        // Jayway filter-expression version concerns.
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        List<?> members = mapper.readValue(json, List.class);

        // Find PM entry and assert phone/email present
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> pm = (java.util.Map<String, Object>) members.stream()
                .filter(m -> "PROJECT_MANAGER".equals(((java.util.Map<?, ?>) m).get("role")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("PROJECT_MANAGER member not found in response"));

        assertThat(pm.get("phone")).isEqualTo("+91-1111");
        assertThat(pm.get("email")).isEqualTo("pm@x.com");

        // Find Architect entry and assert phone/email null/absent
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> arch = (java.util.Map<String, Object>) members.stream()
                .filter(m -> "ARCHITECT".equals(((java.util.Map<?, ?>) m).get("role")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("ARCHITECT member not found in response"));

        assertThat(arch.get("phone")).isNull();
        assertThat(arch.get("email")).isNull();
    }

    /**
     * RED: no handler yet; 404 expected after Task 10 because Customer B does
     * not own Customer A's project.
     */
    @Test
    void returns404ForOtherCustomerProject() throws Exception {
        mockMvc.perform(get("/api/dashboard/projects/" + projectUuid + "/team")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerBToken))
                .andExpect(status().isNotFound());
    }

    /**
     * No Authorization header — Spring Security rejects before the dispatcher.
     * Accept 401 or 403 to be resilient to security config changes
     * (mirrors {@link DashboardControllerProjectInfoIT#unauthenticatedReturns401()}).
     */
    @Test
    void returns401Unauthenticated() throws Exception {
        int status = mockMvc.perform(get("/api/dashboard/projects/" + projectUuid + "/team"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }

    /**
     * RED: no handler yet.
     * After Task 10: project has no staff project_members rows → empty array.
     */
    @Test
    void emptyArrayWhenNoTeam() throws Exception {
        // Create a new project with only the customer member (no portal_user_id staff)
        String emptyUuid = UUID.randomUUID().toString();
        Long emptyProjectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) "
                        + "VALUES ('Empty Team Project', ?::uuid, 0) RETURNING id",
                Long.class, emptyUuid);

        Long custAId = jdbc.queryForObject(
                "SELECT id FROM customer_users WHERE email = 'cust-team-a@test.com'", Long.class);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)",
                emptyProjectId, custAId);

        mockMvc.perform(get("/api/dashboard/projects/" + emptyUuid + "/team")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * RED: no handler yet.
     * After Task 10: project's only staff member has a VIEWER role, which is not
     * in the ALLOWED_ROLES list in {@link com.wd.custapi.service.CustomerTeamService}
     * → the endpoint returns an empty array.
     */
    @Test
    void filtersOutDisallowedRoles() throws Exception {
        // Create a new project and add a VIEWER staff member (role not in allowed list)
        String viewerUuid = UUID.randomUUID().toString();
        Long viewerProjectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) "
                        + "VALUES ('Viewer Only Project', ?::uuid, 0) RETURNING id",
                Long.class, viewerUuid);

        Long custAId = jdbc.queryForObject(
                "SELECT id FROM customer_users WHERE email = 'cust-team-a@test.com'", Long.class);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)",
                viewerProjectId, custAId);

        // Add a portal_user with VIEWER role
        Long viewerPortalUserId = jdbc.queryForObject(
                "INSERT INTO portal_users (first_name, last_name, email, phone) "
                        + "VALUES ('Viewer', 'User', 'viewer@x.com', '+91-3333') RETURNING id",
                Long.class);
        jdbc.update(
                "INSERT INTO project_members (project_id, portal_user_id, role_in_project, share_with_customer) "
                        + "VALUES (?, ?, 'VIEWER', TRUE)",
                viewerProjectId, viewerPortalUserId);

        mockMvc.perform(get("/api/dashboard/projects/" + viewerUuid + "/team")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
