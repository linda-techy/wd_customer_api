package com.wd.custapi.repository;

import com.wd.custapi.model.ProjectBaseline;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Read-only access to the project_baseline table that portal-api owns.
 *
 * <p>In the customer-API test environment Hibernate creates the schema from
 * JPA entities (the V119 migration that owns this table lives in
 * wd_portal_api). The customer-API entity intentionally does not map
 * {@code approved_by} — there is no {@code portal_users} JPA entity here, so
 * the FK target would be unsatisfiable. The Hibernate-generated
 * {@code project_baseline} table therefore lacks the {@code approved_by}
 * column entirely; INSERT statements in this test class must not reference it.
 */
class ProjectBaselineRepositoryTest extends TestcontainersPostgresBase {

    @Autowired
    private ProjectBaselineRepository projectBaselineRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void findByProjectIdReturnsRowWhenPresent() {
        long projectId = seedCustomerAndProject("baseline-present@test.com", "Baseline Present");

        jdbc.update(
                "INSERT INTO project_baseline (project_id, approved_at, project_start_date, project_finish_date) "
                        + "VALUES (?, NOW(), '2026-01-01', '2026-08-05')",
                projectId);

        Optional<ProjectBaseline> found = projectBaselineRepository.findByProjectId(projectId);

        assertThat(found).isPresent();
        assertThat(found.get().getProjectFinishDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(found.get().getProjectStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void findByProjectIdReturnsEmptyWhenAbsent() {
        long projectId = seedCustomerAndProject("baseline-absent@test.com", "Baseline Absent");

        Optional<ProjectBaseline> found = projectBaselineRepository.findByProjectId(projectId);

        assertThat(found).isEmpty();
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
