package com.wd.custapi.service;

import com.wd.custapi.dto.ExpectedHandoverDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.ProjectRepository;
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
 * <p>Tests pass an explicit {@code LocalDate today=2026-05-05} via the
 * {@link ExpectedHandoverService#computeAt(Project, LocalDate)} test seam so
 * weeksRemaining assertions are deterministic without needing to fork the
 * shared Spring test context (which would happen if we replaced the Clock
 * bean for this test only).
 *
 * <p>The service takes an already-authorized Project (the controller does
 * the auth lookup). Tests resolve the Project via ProjectRepository directly
 * since the auth path is not what's under test here.
 */
class ExpectedHandoverServiceTest extends TestcontainersPostgresBase {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 5, 5);

    @Autowired
    private ExpectedHandoverService expectedHandoverService;

    @Autowired
    private ProjectRepository projectRepository;

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
        // Order matters — children before parents. The shared container is
        // populated by many other test classes, so we only delete rows seeded
        // by this class (matched on email prefix / project name) to avoid
        // tripping FK constraints to unrelated rows like customer_refresh_tokens.
        jdbc.update("DELETE FROM delay_logs WHERE project_id IN "
                + "(SELECT id FROM customer_projects WHERE name LIKE 'Project alice-%')");
        jdbc.update("DELETE FROM project_baseline WHERE project_id IN "
                + "(SELECT id FROM customer_projects WHERE name LIKE 'Project alice-%')");
        jdbc.update("DELETE FROM tasks WHERE project_id IN "
                + "(SELECT id FROM customer_projects WHERE name LIKE 'Project alice-%')");
        // Drop any project_members owned by our seeded customers OR projects.
        jdbc.update("DELETE FROM project_members WHERE project_id IN "
                + "(SELECT id FROM customer_projects WHERE name LIKE 'Project alice-%')");
        jdbc.update("DELETE FROM project_members WHERE customer_user_id IN "
                + "(SELECT id FROM customer_users WHERE email LIKE 'alice-%')");
        jdbc.update("DELETE FROM customer_projects WHERE name LIKE 'Project alice-%'");
        jdbc.update("DELETE FROM customer_users WHERE email LIKE 'alice-%'");
    }

    @Test
    void compute_noBaselineNoDelay_setsFinishDateAndNullBaseline() {
        UUID projectUuid = UUID.randomUUID();
        long projectId = seedProject(projectUuid, "alice-noBaseline@test.com");
        seedTaskWithEf(projectId, "T1", LocalDate.of(2026, 7, 1));
        seedTaskWithEf(projectId, "T2", LocalDate.of(2026, 8, 12));
        seedTaskWithEf(projectId, "T3", LocalDate.of(2026, 6, 15));

        ExpectedHandoverDto dto = expectedHandoverService.computeAt(loadProject(projectUuid), FIXED_TODAY);

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

        ExpectedHandoverDto dto = expectedHandoverService.computeAt(loadProject(projectUuid), FIXED_TODAY);

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

        ExpectedHandoverDto dto = expectedHandoverService.computeAt(loadProject(projectUuid), FIXED_TODAY);

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

        ExpectedHandoverDto dto = expectedHandoverService.computeAt(loadProject(projectUuid), FIXED_TODAY);

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

        ExpectedHandoverDto dto = expectedHandoverService.computeAt(loadProject(projectUuid), FIXED_TODAY);

        assertThat(dto.hasMaterialDelay())
                .as("internal-only material delays must not leak to customer flag")
                .isFalse();
    }

    @Test
    void compute_oldMaterialThenRecentMinor_doesNotFlag() {
        // Spec: hasMaterialDelay reflects the LATEST customer-visible delay,
        // not "any historical MATERIAL ever". A project that had a MATERIAL
        // delay 30 days ago and now has a MINOR delay this week must NOT
        // show the struck-through baseline indicator.
        UUID projectUuid = UUID.randomUUID();
        long projectId = seedProject(projectUuid, "alice-supersede@test.com");
        seedTaskWithEf(projectId, "T1", LocalDate.of(2026, 8, 12));
        // Old MATERIAL delay (30 days ago)
        seedDelayLogOnDate(projectId, "MATERIAL", true, FIXED_TODAY.minusDays(30));
        // Recent MINOR delay (7 days ago) — supersedes the MATERIAL row.
        seedDelayLogOnDate(projectId, "MINOR", true, FIXED_TODAY.minusDays(7));

        ExpectedHandoverDto dto = expectedHandoverService.computeAt(loadProject(projectUuid), FIXED_TODAY);

        assertThat(dto.hasMaterialDelay())
                .as("only the latest customer-visible delay drives the flag")
                .isFalse();
    }

    // --- helpers ---
    private Project loadProject(UUID projectUuid) {
        Project project = projectRepository.findByProjectUuid(projectUuid);
        assertThat(project).as("seeded project must be loadable").isNotNull();
        return project;
    }

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

    private void seedDelayLogOnDate(long projectId, String impactOnHandover,
                                    boolean customerVisible, LocalDate fromDate) {
        jdbc.update(
                "INSERT INTO delay_logs (project_id, delay_type, from_date, impact_on_handover, customer_visible, created_at) "
                        + "VALUES (?, 'WEATHER', ?, ?, ?, NOW())",
                projectId, fromDate, impactOnHandover, customerVisible);
    }
}
