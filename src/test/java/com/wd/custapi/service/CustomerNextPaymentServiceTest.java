package com.wd.custapi.service;

import com.wd.custapi.dto.NextPaymentMilestoneDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerNextPaymentServiceTest extends TestcontainersPostgresBase {

    private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 5, 10);

    @Autowired private CustomerNextPaymentService service;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        // payment_stages must go before customer_projects (FK).
        jdbc.update("DELETE FROM payment_stages WHERE project_id IN " +
                "(SELECT id FROM customer_projects WHERE name LIKE 'Project bob-%')");
        jdbc.update("DELETE FROM project_members WHERE project_id IN " +
                "(SELECT id FROM customer_projects WHERE name LIKE 'Project bob-%')");
        jdbc.update("DELETE FROM project_members WHERE customer_user_id IN " +
                "(SELECT id FROM customer_users WHERE email LIKE 'bob-%')");
        jdbc.update("DELETE FROM customer_projects WHERE name LIKE 'Project bob-%'");
        jdbc.update("DELETE FROM customer_users WHERE email LIKE 'bob-%'");
    }

    // ----- Helpers -----

    private long seedProject(UUID projectUuid, String email) {
        jdbc.update("INSERT INTO customer_roles (id, name) VALUES (1, 'CUSTOMER') ON CONFLICT DO NOTHING");
        jdbc.update("INSERT INTO customer_users (email, password, first_name, role_id, created_at, enabled) " +
                "VALUES (?, 'x', 'Test', 1, now(), true) ON CONFLICT (email) DO NOTHING", email);
        Long projectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (name, project_uuid, version) VALUES (?, ?::uuid, 0) RETURNING id",
                Long.class, "Project " + email, projectUuid.toString());
        Long customerId = jdbc.queryForObject(
                "SELECT id FROM customer_users WHERE email = ?", Long.class, email);
        jdbc.update("INSERT INTO project_members (project_id, customer_user_id) VALUES (?, ?)",
                projectId, customerId);
        return projectId;
    }

    private void seedStage(long projectId, int n, String name, String status,
                           LocalDate dueDate, BigDecimal netPayable, BigDecimal stagePct) {
        // Hibernate ddl-auto=create generated payment_stages from JPA — most
        // numeric columns are nullable in the JPA model so we set just what
        // we need for the test (status is non-null).
        jdbc.update("INSERT INTO payment_stages (project_id, stage_number, stage_name, status, " +
                "due_date, net_payable_amount, stage_percentage, stage_amount_incl_gst) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                projectId, n, name, status, dueDate, netPayable, stagePct, netPayable);
    }

    private Project loadProject(UUID uuid) {
        Project p = projectRepository.findByProjectUuid(uuid);
        assertThat(p).as("seeded project must be loadable").isNotNull();
        return p;
    }

    // ----- Tests -----

    @Test
    void mixedStages_returnsLowestNonTerminalAsNext() {
        UUID uuid = UUID.randomUUID();
        long pid = seedProject(uuid, "bob-mixed@test.com");
        // 1 PAID, 2 INVOICED, 1 DUE, 1 UPCOMING — spec fixture
        seedStage(pid, 1, "Booking",      "PAID",     LocalDate.of(2026, 1, 10), new BigDecimal("100000"), new BigDecimal("10"));
        seedStage(pid, 2, "Excavation",   "INVOICED", LocalDate.of(2026, 2, 10), new BigDecimal("150000"), new BigDecimal("15"));
        seedStage(pid, 3, "Foundation",   "INVOICED", LocalDate.of(2026, 3, 10), new BigDecimal("200000"), new BigDecimal("20"));
        seedStage(pid, 4, "Plastering",   "DUE",      LocalDate.of(2026, 5, 15), new BigDecimal("425000"), new BigDecimal("12"));
        seedStage(pid, 5, "Finishing",    "UPCOMING", LocalDate.of(2026, 7, 1),  new BigDecimal("125000"), new BigDecimal("12.5"));

        NextPaymentMilestoneDto dto = service.getNextPaymentMilestoneAt(loadProject(uuid), FIXED_TODAY);

        assertThat(dto.stage()).isNotNull();
        // "Next" is the lowest stage_number where status NOT IN (PAID, ON_HOLD).
        // Stage 1 is PAID → skip. Stage 2 is INVOICED (not terminal here) → returned.
        assertThat(dto.stage().stageNumber()).isEqualTo(2);
        assertThat(dto.stage().stageName()).isEqualTo("Excavation");
        assertThat(dto.stage().status()).isEqualTo("INVOICED");
        assertThat(dto.stage().totalStages()).isEqualTo(5);
    }

    @Test
    void allPaid_returnsNullStageWithSummary() {
        UUID uuid = UUID.randomUUID();
        long pid = seedProject(uuid, "bob-allpaid@test.com");
        seedStage(pid, 1, "S1", "PAID", LocalDate.of(2026, 1, 10), new BigDecimal("500000"), new BigDecimal("50"));
        seedStage(pid, 2, "S2", "PAID", LocalDate.of(2026, 2, 10), new BigDecimal("500000"), new BigDecimal("50"));

        NextPaymentMilestoneDto dto = service.getNextPaymentMilestoneAt(loadProject(uuid), FIXED_TODAY);

        assertThat(dto.stage()).isNull();
        assertThat(dto.summary()).isNotNull();
        assertThat(dto.summary().stageCount()).isEqualTo(2);
    }

    @Test
    void allOnHold_returnsNullStage() {
        UUID uuid = UUID.randomUUID();
        long pid = seedProject(uuid, "bob-allhold@test.com");
        seedStage(pid, 1, "S1", "ON_HOLD", LocalDate.of(2026, 1, 10), new BigDecimal("500000"), new BigDecimal("50"));
        seedStage(pid, 2, "S2", "ON_HOLD", LocalDate.of(2026, 2, 10), new BigDecimal("500000"), new BigDecimal("50"));

        NextPaymentMilestoneDto dto = service.getNextPaymentMilestoneAt(loadProject(uuid), FIXED_TODAY);

        assertThat(dto.stage()).as("ON_HOLD treated like PAID for 'next' selection").isNull();
    }

    @Test
    void mixedPaidOnHoldAndDue_returnsTheDueStage() {
        UUID uuid = UUID.randomUUID();
        long pid = seedProject(uuid, "bob-mix2@test.com");
        seedStage(pid, 1, "S1", "PAID",    LocalDate.of(2026, 1, 10), new BigDecimal("100000"), new BigDecimal("10"));
        seedStage(pid, 2, "S2", "ON_HOLD", LocalDate.of(2026, 2, 10), new BigDecimal("100000"), new BigDecimal("10"));
        seedStage(pid, 3, "S3", "DUE",     LocalDate.of(2026, 5, 15), new BigDecimal("400000"), new BigDecimal("40"));
        seedStage(pid, 4, "S4", "UPCOMING",LocalDate.of(2026, 7, 1),  new BigDecimal("400000"), new BigDecimal("40"));

        NextPaymentMilestoneDto dto = service.getNextPaymentMilestoneAt(loadProject(uuid), FIXED_TODAY);

        assertThat(dto.stage()).isNotNull();
        assertThat(dto.stage().stageNumber()).isEqualTo(3);
        assertThat(dto.stage().status()).isEqualTo("DUE");
    }

    @Test
    void daysUntilDue_isPositiveBeforeDueDate() {
        UUID uuid = UUID.randomUUID();
        long pid = seedProject(uuid, "bob-future@test.com");
        // stage_percentage column is precision=6, scale=4 (max < 100). Use 99.9999.
        seedStage(pid, 1, "S1", "DUE", LocalDate.of(2026, 5, 15), new BigDecimal("100000"), new BigDecimal("99.9999"));

        NextPaymentMilestoneDto dto = service.getNextPaymentMilestoneAt(loadProject(uuid), FIXED_TODAY);

        // FIXED_TODAY = 2026-05-10, due = 2026-05-15 → 5 days.
        assertThat(dto.stage().daysUntilDue()).isEqualTo(5);
    }

    @Test
    void daysUntilDue_isNegativeWhenOverdue() {
        UUID uuid = UUID.randomUUID();
        long pid = seedProject(uuid, "bob-overdue@test.com");
        // stage_percentage column is precision=6, scale=4 (max < 100). Use 99.9999.
        seedStage(pid, 1, "S1", "OVERDUE", LocalDate.of(2026, 5, 8), new BigDecimal("100000"), new BigDecimal("99.9999"));

        NextPaymentMilestoneDto dto = service.getNextPaymentMilestoneAt(loadProject(uuid), FIXED_TODAY);

        // FIXED_TODAY = 2026-05-10, due = 2026-05-08 → -2 days.
        assertThat(dto.stage().daysUntilDue()).isEqualTo(-2);
    }

    @Test
    void dueDateNull_yieldsNullDaysUntilDue() {
        UUID uuid = UUID.randomUUID();
        long pid = seedProject(uuid, "bob-nodate@test.com");
        // stage_percentage column is precision=6, scale=4 (max < 100). Use 99.9999.
        seedStage(pid, 1, "S1", "UPCOMING", null, new BigDecimal("100000"), new BigDecimal("99.9999"));

        NextPaymentMilestoneDto dto = service.getNextPaymentMilestoneAt(loadProject(uuid), FIXED_TODAY);

        assertThat(dto.stage()).isNotNull();
        assertThat(dto.stage().dueDate()).isNull();
        assertThat(dto.stage().daysUntilDue()).as("null due_date → null daysUntilDue, never throws").isNull();
    }

    @Test
    void percentOfContract_isRecomputedFromContractTotal() {
        UUID uuid = UUID.randomUUID();
        long pid = seedProject(uuid, "bob-pct@test.com");
        // total contract = 1,000,000; this stage net = 250,000 → 25.0%
        seedStage(pid, 1, "S1", "PAID",     LocalDate.of(2026, 1, 1), new BigDecimal("250000"), new BigDecimal("25"));
        seedStage(pid, 2, "S2", "DUE",      LocalDate.of(2026, 5, 15), new BigDecimal("250000"), new BigDecimal("25"));
        seedStage(pid, 3, "S3", "UPCOMING", LocalDate.of(2026, 7, 1),  new BigDecimal("500000"), new BigDecimal("50"));

        NextPaymentMilestoneDto dto = service.getNextPaymentMilestoneAt(loadProject(uuid), FIXED_TODAY);

        // The "next" is stage 2 (PAID skipped). Recomputed pct = 250000 / 1000000 * 100 = 25.0
        assertThat(dto.stage().stageNumber()).isEqualTo(2);
        assertThat(dto.stage().percentOfContract()).isEqualByComparingTo(new BigDecimal("25.0"));
        assertThat(dto.summary().totalContractValue()).isEqualByComparingTo(new BigDecimal("1000000"));
    }

    @Test
    void emptySchedule_returnsNullStageAndZeroSummary() {
        UUID uuid = UUID.randomUUID();
        seedProject(uuid, "bob-empty@test.com");
        // No stages seeded.

        NextPaymentMilestoneDto dto = service.getNextPaymentMilestoneAt(loadProject(uuid), FIXED_TODAY);

        assertThat(dto.stage()).isNull();
        assertThat(dto.summary().stageCount()).isZero();
        assertThat(dto.summary().totalContractValue()).isEqualByComparingTo("0");
    }
}
