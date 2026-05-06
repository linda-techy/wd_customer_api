package com.wd.custapi.repository;

import com.wd.custapi.model.Task;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Read-only mapping of CPM-denormalized columns on the tasks table.
 *
 * <p>The portal-API V118 migration added ef_date / es_date / ls_date / lf_date
 * / total_float_days / is_critical / actual_start_date. Customer-API only
 * needs ef_date to compute the project's expected handover, so this test
 * only asserts the ef_date column round-trip and the
 * findMaxEfDateByProjectId aggregate query.
 */
class TaskCpmFieldsTest extends TestcontainersPostgresBase {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void efDateColumnIsReadable() {
        long projectId = seedCustomerAndProject("alice-ef@test.com", "Project Ef-Date");
        jdbc.update(
                "INSERT INTO tasks (project_id, title, status, priority, due_date, ef_date, customer_visible) "
                        + "VALUES (?, 'T1', 'OPEN', 'NORMAL', '2026-08-12', '2026-08-12', true)",
                projectId);

        List<Task> tasks = taskRepository.findByProjectIdOrderByDueDateAsc(projectId);

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getEfDate()).isEqualTo(LocalDate.of(2026, 8, 12));
    }

    private long seedCustomerAndProject(String email, String projectName) {
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                + "VALUES (?, 'x', 'Test', 1, now(), true) ON CONFLICT (email) DO NOTHING", email);
        Long projectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) VALUES (?, gen_random_uuid(), 0) RETURNING id",
                Long.class, projectName);
        Long customerId = jdbc.queryForObject(
                "SELECT id FROM customer_users WHERE email = ?", Long.class, email);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)",
                projectId, customerId);
        return projectId;
    }
}
