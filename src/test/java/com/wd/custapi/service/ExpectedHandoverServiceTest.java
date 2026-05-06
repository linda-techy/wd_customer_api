package com.wd.custapi.service;

import com.wd.custapi.dto.ExpectedHandoverDto;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end Service test for ExpectedHandoverService — uses Postgres
 * Testcontainer to seed real rows in tasks, project_baseline, and delay_logs
 * (so the JPA queries on real columns are exercised).
 *
 * <p>Today's date is pinned to 2026-05-05 via a test {@code Clock} bean
 * defined in {@link TestcontainersPostgresBase} so weeksRemaining assertions
 * remain deterministic regardless of when the suite runs.
 */
class ExpectedHandoverServiceTest extends TestcontainersPostgresBase {

    @Autowired
    private ExpectedHandoverService expectedHandoverService;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        if (cacheManager.getCache("expectedHandover") != null) {
            cacheManager.getCache("expectedHandover").clear();
        }
    }

    @AfterEach
    void cleanup() {
        // Order matters — children before parents.
        jdbc.update("DELETE FROM delay_logs");
        jdbc.update("DELETE FROM project_baseline");
        jdbc.update("DELETE FROM tasks");
        jdbc.update("DELETE FROM project_members");
        jdbc.update("DELETE FROM customer_projects");
        jdbc.update("DELETE FROM customer_users");
    }

    @Test
    void compute_noBaselineNoDelay_setsFinishDateAndNullBaseline() {
        UUID projectUuid = UUID.randomUUID();
        long projectId = seedProject(projectUuid, "alice-noBaseline@test.com");
        seedTaskWithEf(projectId, "T1", LocalDate.of(2026, 7, 1));
        seedTaskWithEf(projectId, "T2", LocalDate.of(2026, 8, 12));
        seedTaskWithEf(projectId, "T3", LocalDate.of(2026, 6, 15));

        ExpectedHandoverDto dto = expectedHandoverService.compute(projectUuid.toString());

        assertThat(dto.projectFinishDate()).isEqualTo(LocalDate.of(2026, 8, 12));
        assertThat(dto.baselineFinishDate()).isNull();
        assertThat(dto.hasMaterialDelay()).isFalse();
        assertThat(dto.weeksRemaining()).isNotNull();
        // Mon-Sat working days from 2026-05-05 -> 2026-08-12 / 5 rounded.
        // Range = 99 days; 14 of those are Sundays → 85 working days; round(85/5) = 17.
        assertThat(dto.weeksRemaining()).isEqualTo(17);
    }

    @Test
    void compute_baselineExistsButOnlyMinorDelay_returnsAllFieldsNoMaterialFlag() {
        UUID projectUuid = UUID.randomUUID();
        long projectId = seedProject(projectUuid, "alice-minor@test.com");
        seedTaskWithEf(projectId, "T1", LocalDate.of(2026, 8, 12));
        seedBaseline(projectId, LocalDate.of(2026, 8, 5));
        seedDelayLog(projectId, "MINOR", true);

        ExpectedHandoverDto dto = expectedHandoverService.compute(projectUuid.toString());

        assertThat(dto.projectFinishDate()).isEqualTo(LocalDate.of(2026, 8, 12));
        assertThat(dto.baselineFinishDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(dto.hasMaterialDelay()).isFalse();
    }

    @Test
    void compute_baselineAndMaterialDelay_setsAllThreeFields() {
        UUID projectUuid = UUID.randomUUID();
        long projectId = seedProject(projectUuid, "alice-material@test.com");
        seedTaskWithEf(projectId, "T1", LocalDate.of(2026, 8, 12));
        seedBaseline(projectId, LocalDate.of(2026, 8, 5));
        seedDelayLog(projectId, "MATERIAL", true);

        ExpectedHandoverDto dto = expectedHandoverService.compute(projectUuid.toString());

        assertThat(dto.projectFinishDate()).isEqualTo(LocalDate.of(2026, 8, 12));
        assertThat(dto.baselineFinishDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(dto.hasMaterialDelay()).isTrue();
    }

    @Test
    void compute_noTasksWithEfDate_returnsNullFinishDateAndNullWeeks() {
        UUID projectUuid = UUID.randomUUID();
        long projectId = seedProject(projectUuid, "alice-noef@test.com");
        // Task without ef_date — represents the CPM-not-yet-run case.
        jdbc.update("INSERT INTO tasks (project_id, title, status, priority, due_date, customer_visible) "
                + "VALUES (?, 'T1', 'OPEN', 'NORMAL', '2026-08-12', true)", projectId);

        ExpectedHandoverDto dto = expectedHandoverService.compute(projectUuid.toString());

        assertThat(dto.projectFinishDate()).isNull();
        assertThat(dto.weeksRemaining()).isNull();
        assertThat(dto.baselineFinishDate()).isNull();
        assertThat(dto.hasMaterialDelay()).isFalse();
    }

    @Test
    void compute_materialDelayMarkedNotCustomerVisible_doesNotFlag() {
        UUID projectUuid = UUID.randomUUID();
        long projectId = seedProject(projectUuid, "alice-internal@test.com");
        seedTaskWithEf(projectId, "T1", LocalDate.of(2026, 8, 12));
        seedDelayLog(projectId, "MATERIAL", false);

        ExpectedHandoverDto dto = expectedHandoverService.compute(projectUuid.toString());

        assertThat(dto.hasMaterialDelay())
                .as("internal-only material delays must not leak to customer flag")
                .isFalse();
    }

    // --- helpers ---
    private long seedProject(UUID projectUuid, String email) {
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) "
                + "VALUES (?, 'x', 'Test', 1, now(), true) ON CONFLICT (email) DO NOTHING", email);
        Long projectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) VALUES (?, ?::uuid, 0) RETURNING id",
                Long.class, "Project " + email, projectUuid.toString());
        Long customerId = jdbc.queryForObject(
                "SELECT id FROM customer_users WHERE email = ?", Long.class, email);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)",
                projectId, customerId);
        return projectId;
    }

    private void seedTaskWithEf(long projectId, String title, LocalDate efDate) {
        jdbc.update(
                "INSERT INTO tasks (project_id, title, status, priority, due_date, ef_date, customer_visible) "
                        + "VALUES (?, ?, 'OPEN', 'NORMAL', ?, ?, true)",
                projectId, title, efDate, efDate);
    }

    private void seedBaseline(long projectId, LocalDate finishDate) {
        jdbc.update(
                "INSERT INTO project_baseline (project_id, approved_at, project_start_date, project_finish_date) "
                        + "VALUES (?, NOW(), '2026-01-01', ?)",
                projectId, finishDate);
    }

    private void seedDelayLog(long projectId, String impactOnHandover, boolean customerVisible) {
        jdbc.update(
                "INSERT INTO delay_logs (project_id, delay_type, from_date, impact_on_handover, customer_visible, created_at) "
                        + "VALUES (?, 'WEATHER', '2026-04-01', ?, ?, NOW())",
                projectId, impactOnHandover, customerVisible);
    }
}
