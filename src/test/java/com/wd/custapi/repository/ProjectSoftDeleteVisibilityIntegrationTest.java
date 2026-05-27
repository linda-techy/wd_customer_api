package com.wd.custapi.repository;

import com.wd.custapi.config.TestDataSeeder;
import com.wd.custapi.model.Project;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression lock: customer-facing project queries must NOT return soft-deleted
 * projects.
 *
 * <p>These queries are raw native SQL ({@code nativeQuery = true}), which bypasses
 * the entity-level {@code @SQLRestriction("deleted_at IS NULL")} on {@link Project}.
 * So each must filter {@code deleted_at} itself, or a soft-deleted project leaks to
 * the customer. That leak let customer krishnan.kerala stay bound to a soft-deleted
 * duplicate ({@code PRJ-2026-0051}) — visible in the customer app but invisible/
 * unmanageable from the portal — masking that they weren't linked to the live
 * project (audit, 2026-05-27).
 *
 * <p>Intentionally NOT {@code @Transactional}: like {@code CustomerReadOsivLazyLoadingIntegrationTest},
 * it lets {@link TestDataSeeder#seed()} COMMIT its baseline roles. A rolled-back
 * seed advances the {@code customer_roles} identity sequence without committing
 * role id 1, which breaks sibling tests that hard-code {@code role_id = 1}. The
 * shared project is soft-deleted and then RESTORED in a finally block so this test
 * leaves no residue.
 */
class ProjectSoftDeleteVisibilityIntegrationTest extends TestcontainersPostgresBase {

    @Autowired private TestDataSeeder seeder;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final String EMAIL = "customerA@test.com";
    private Long projectId;

    @BeforeEach
    void setUp() {
        seeder.seed();
        projectId = seeder.getResidentialVilla().getId(); // committed, linked to customerA
    }

    @Test
    void customerQueries_excludeSoftDeletedProjects() {
        // Sanity: the live project is visible to its customer.
        assertThat(projectRepository.findAllByCustomerEmail(EMAIL))
                .extracting(Project::getId).contains(projectId);

        try {
            // Soft-delete by setting deleted_at directly (mirrors @SQLDelete without
            // the versioned-delete parameter dance). Commits, so the native customer
            // queries below — which run in their own transactions — observe it.
            jdbcTemplate.update("UPDATE customer_projects SET deleted_at = NOW() WHERE id = ?", projectId);

            // Every customer-facing query must now EXCLUDE the soft-deleted project.
            assertThat(projectRepository.findAllByCustomerEmail(EMAIL))
                    .extracting(Project::getId).doesNotContain(projectId);

            assertThat(projectRepository.findRecentByCustomerEmail(EMAIL, 20))
                    .extracting(Project::getId).doesNotContain(projectId);

            assertThat(projectRepository.findByIdAndCustomerEmail(projectId, EMAIL))
                    .as("findByIdAndCustomerEmail must not resolve a soft-deleted project")
                    .isNull();
        } finally {
            // Restore so sibling tests (and reseeds) see the shared project live.
            jdbcTemplate.update("UPDATE customer_projects SET deleted_at = NULL WHERE id = ?", projectId);
        }
    }
}
