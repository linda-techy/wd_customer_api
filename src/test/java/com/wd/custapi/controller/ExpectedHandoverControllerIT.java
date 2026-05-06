package com.wd.custapi.controller;

import com.wd.custapi.service.JwtService;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
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
 * End-to-end integration smoke for the expected-handover endpoint —
 * exercises the full path: security filter -> controller -> service ->
 * repositories -> Postgres Testcontainer.
 */
@AutoConfigureMockMvc
class ExpectedHandoverControllerIT extends TestcontainersPostgresBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CacheManager cacheManager;

    private String customerToken;
    private String projectUuid;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("expectedHandover") != null) {
            cacheManager.getCache("expectedHandover").clear();
        }
        // Targeted cleanup of just our seeded rows (the shared Postgres
        // container is populated by many test classes; broad DELETEs trip
        // unrelated FK constraints like customer_refresh_tokens).
        jdbc.update("DELETE FROM delay_logs WHERE project_id IN "
                + "(SELECT id FROM customer_projects WHERE name = 'Handover IT')");
        jdbc.update("DELETE FROM project_baseline WHERE project_id IN "
                + "(SELECT id FROM customer_projects WHERE name = 'Handover IT')");
        jdbc.update("DELETE FROM tasks WHERE project_id IN "
                + "(SELECT id FROM customer_projects WHERE name = 'Handover IT')");
        jdbc.update("DELETE FROM project_members WHERE project_id IN "
                + "(SELECT id FROM customer_projects WHERE name = 'Handover IT')");
        jdbc.update("DELETE FROM customer_projects WHERE name = 'Handover IT'");
        jdbc.update("DELETE FROM customer_users WHERE email = 'handover-cust@test.com'");

        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                + "VALUES ('handover-cust@test.com', 'x', 'Cust', 1, now(), true)");
        Long custId = jdbc.queryForObject(
                "SELECT id FROM customer_users WHERE email = 'handover-cust@test.com'", Long.class);

        projectUuid = UUID.randomUUID().toString();
        Long projectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) VALUES ('Handover IT', ?::uuid, 0) RETURNING id",
                Long.class, projectUuid);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)", projectId, custId);

        // Two tasks with ef_date — max determines projectFinishDate.
        jdbc.update("INSERT INTO tasks (project_id, title, status, priority, due_date, ef_date, customer_visible) "
                + "VALUES (?, 'T1', 'OPEN', 'NORMAL', '2026-08-12', '2026-08-12', true)", projectId);
        jdbc.update("INSERT INTO tasks (project_id, title, status, priority, due_date, ef_date, customer_visible) "
                + "VALUES (?, 'T2', 'OPEN', 'NORMAL', '2026-07-01', '2026-07-01', true)", projectId);

        // A customer-visible MATERIAL delay -> hasMaterialDelay must be true.
        jdbc.update("INSERT INTO delay_logs (project_id, delay_type, from_date, impact_on_handover, customer_visible, created_at) "
                + "VALUES (?, 'WEATHER', '2026-04-01', 'MATERIAL', true, NOW())", projectId);

        customerToken = jwtService.generateCustomerToken("handover-cust@test.com", new HashMap<>());
    }

    @Test
    void endToEndReturnsExpectedShape() throws Exception {
        String body = mockMvc.perform(get("/api/customer/projects/" + projectUuid + "/expected-handover")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectFinishDate").value("2026-08-12"))
                .andExpect(jsonPath("$.weeksRemaining").isNumber())
                .andExpect(jsonPath("$.hasMaterialDelay").value(true))
                .andReturn().getResponse().getContentAsString();

        // baselineFinishDate is serialised as JSON null (no baseline seeded).
        // jsonPath cannot reliably match null keys with isEmpty()/value(null),
        // so we assert the four expected keys are present in the response body.
        assertThat(body).contains("\"projectFinishDate\"");
        assertThat(body).contains("\"baselineFinishDate\"");
        assertThat(body).contains("\"weeksRemaining\"");
        assertThat(body).contains("\"hasMaterialDelay\"");
        // baselineFinishDate must serialise as null since no baseline row exists.
        assertThat(body).contains("\"baselineFinishDate\":null");
    }
}
