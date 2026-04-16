package com.wd.custapi.service;

import com.wd.custapi.model.Project;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardServiceProjectByIdAndEmailTest extends TestcontainersPostgresBase {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void ownerCanAccessTheirOwnProject() {
        long projectId = seedCustomerAndProject("alice@test.com", "Alice Project");
        Project found = dashboardService.getProjectByIdAndEmail(projectId, "alice@test.com");
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(projectId);
    }

    @Test
    void nonOwnerGetsException() {
        long projectId = seedCustomerAndProject("alice@test.com", "Alice Project");
        seedCustomerOnly("bob@test.com");
        assertThatThrownBy(() -> dashboardService.getProjectByIdAndEmail(projectId, "bob@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void adminCanAccessAnyProject() {
        long projectId = seedCustomerAndProject("alice@test.com", "Alice Project");
        seedAdminCustomer("admin@test.com");
        Project found = dashboardService.getProjectByIdAndEmail(projectId, "admin@test.com");
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(projectId);
    }

    // --- helpers ---
    private long seedCustomerAndProject(String email, String projectName) {
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) VALUES (?, 'x', 'Test', 1, now(), true) ON CONFLICT (email) DO NOTHING", email);
        Long projectId = jdbc.queryForObject("INSERT INTO customer_projects (name, project_uuid, version) VALUES (?, gen_random_uuid(), 0) RETURNING id", Long.class, projectName);
        Long customerId = jdbc.queryForObject("SELECT id FROM customer_users WHERE email = ?", Long.class, email);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)", projectId, customerId);
        return projectId;
    }

    private void seedCustomerOnly(String email) {
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) VALUES (?, 'x', 'Test', 1, now(), true) ON CONFLICT (email) DO NOTHING", email);
    }

    private void seedAdminCustomer(String email) {
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (2, 'ADMIN') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) VALUES (?, 'x', 'Admin', 2, now(), true) ON CONFLICT (email) DO NOTHING", email);
    }
}
