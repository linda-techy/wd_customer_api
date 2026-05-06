package com.wd.custapi.service;

import com.wd.custapi.dto.ExpectedHandoverDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.ProjectBaseline;
import com.wd.custapi.repository.DelayLogRepository;
import com.wd.custapi.repository.ProjectBaselineRepository;
import com.wd.custapi.repository.TaskRepository;
import com.wd.custapi.util.WorkingDayCalculator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Aggregates the data needed by the customer-facing expected-handover row:
 *
 * <ul>
 *   <li>{@code projectFinishDate} — max {@code task.ef_date} for the project
 *       (the CPM-denormalized column populated by portal-api's CpmService).</li>
 *   <li>{@code baselineFinishDate} — {@code project_baseline.project_finish_date}
 *       if the project has an approved baseline.</li>
 *   <li>{@code hasMaterialDelay} — true iff the LATEST customer-visible delay
 *       log row has {@code impact_on_handover='MATERIAL'}.</li>
 *   <li>{@code weeksRemaining} — Mon-Sat working days from "today" to
 *       {@code projectFinishDate}, divided by 5 and rounded.</li>
 * </ul>
 *
 * <p>Cached 5 min via the existing Caffeine CacheManager (cache name
 * {@code expectedHandover}) — see {@link com.wd.custapi.config.CacheConfig}.
 *
 * <p>The service takes an already-authorized {@link Project} from the
 * controller — it intentionally does NOT re-resolve via
 * {@code projectRepository.findByProjectUuid} (which is admin-unscoped).
 * This removes the structural footgun where a future non-controller caller
 * could bypass the controller's
 * {@link DashboardService#getProjectByUuidAndEmail} pre-auth and saves a
 * DB roundtrip.
 *
 * <p>Tests inject "today" via {@link #computeAt(Project, LocalDate)}; the
 * production endpoint calls {@link #compute(Project)} which delegates to
 * {@code computeAt} with {@link LocalDate#now()}.
 */
@Service
public class ExpectedHandoverService {

    private final TaskRepository taskRepository;
    private final ProjectBaselineRepository projectBaselineRepository;
    private final DelayLogRepository delayLogRepository;

    public ExpectedHandoverService(
            TaskRepository taskRepository,
            ProjectBaselineRepository projectBaselineRepository,
            DelayLogRepository delayLogRepository) {
        this.taskRepository = taskRepository;
        this.projectBaselineRepository = projectBaselineRepository;
        this.delayLogRepository = delayLogRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "expectedHandover", key = "#project.projectUuid")
    public ExpectedHandoverDto compute(Project project) {
        return computeAt(project, LocalDate.now());
    }

    /**
     * Test seam — same as {@link #compute(Project)} but with an explicit
     * "today" date so weeksRemaining assertions are deterministic. NOT
     * cached (caching the test seam would let an unrelated cache eviction
     * leak the test date into a production response).
     */
    @Transactional(readOnly = true)
    public ExpectedHandoverDto computeAt(Project project, LocalDate today) {
        Long projectId = project.getId();

        LocalDate projectFinishDate = taskRepository
                .findMaxEfDateByProjectId(projectId)
                .orElse(null);

        LocalDate baselineFinishDate = projectBaselineRepository
                .findByProjectId(projectId)
                .map(ProjectBaseline::getProjectFinishDate)
                .orElse(null);

        // Spec: hasMaterialDelay reflects the LATEST customer-visible delay,
        // not "any historical MATERIAL ever". Take the head of the
        // from-date-desc list and check only that row's impact.
        boolean hasMaterialDelay = delayLogRepository
                .findByProjectIdAndCustomerVisibleTrueOrderByFromDateDesc(projectId)
                .stream()
                .findFirst()
                .map(d -> "MATERIAL".equalsIgnoreCase(d.getImpactOnHandover()))
                .orElse(false);

        Integer weeksRemaining = null;
        if (projectFinishDate != null) {
            int days = WorkingDayCalculator.workingDaysBetween(today, projectFinishDate);
            weeksRemaining = (int) Math.round(days / 5.0);
        }

        return new ExpectedHandoverDto(projectFinishDate, baselineFinishDate, weeksRemaining, hasMaterialDelay);
    }
}
