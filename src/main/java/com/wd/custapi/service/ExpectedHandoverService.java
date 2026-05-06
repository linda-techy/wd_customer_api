package com.wd.custapi.service;

import com.wd.custapi.dto.ExpectedHandoverDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.ProjectBaseline;
import com.wd.custapi.repository.DelayLogRepository;
import com.wd.custapi.repository.ProjectBaselineRepository;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.TaskRepository;
import com.wd.custapi.util.WorkingDayCalculator;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Aggregates the data needed by the customer-facing expected-handover row:
 *
 * <ul>
 *   <li>{@code projectFinishDate} — max {@code task.ef_date} for the project
 *       (the CPM-denormalized column populated by portal-api's CpmService).</li>
 *   <li>{@code baselineFinishDate} — {@code project_baseline.project_finish_date}
 *       if the project has an approved baseline.</li>
 *   <li>{@code hasMaterialDelay} — any customer-visible delay log row whose
 *       {@code impact_on_handover='MATERIAL'}.</li>
 *   <li>{@code weeksRemaining} — Mon-Sat working days from "today" (injected
 *       Clock) to {@code projectFinishDate}, divided by 5 and rounded.</li>
 * </ul>
 *
 * <p>Cached 5 min via the existing Caffeine CacheManager (cache name
 * {@code expectedHandover}) — see {@link com.wd.custapi.config.CacheConfig}.
 */
@Service
public class ExpectedHandoverService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final ProjectBaselineRepository projectBaselineRepository;
    private final DelayLogRepository delayLogRepository;
    private final Clock clock;

    public ExpectedHandoverService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            ProjectBaselineRepository projectBaselineRepository,
            DelayLogRepository delayLogRepository,
            Clock clock) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.projectBaselineRepository = projectBaselineRepository;
        this.delayLogRepository = delayLogRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "expectedHandover", key = "#projectUuid")
    public ExpectedHandoverDto compute(String projectUuid) {
        UUID uuid;
        try {
            uuid = UUID.fromString(projectUuid);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid project UUID format: " + projectUuid);
        }
        Project project = projectRepository.findByProjectUuid(uuid);
        if (project == null) {
            throw new RuntimeException("Project not found: " + projectUuid);
        }
        Long projectId = project.getId();

        LocalDate projectFinishDate = taskRepository
                .findMaxEfDateByProjectId(projectId)
                .orElse(null);

        LocalDate baselineFinishDate = projectBaselineRepository
                .findByProjectId(projectId)
                .map(ProjectBaseline::getProjectFinishDate)
                .orElse(null);

        boolean hasMaterialDelay = delayLogRepository
                .findByProjectIdAndCustomerVisibleTrueOrderByFromDateDesc(projectId)
                .stream()
                .anyMatch(d -> "MATERIAL".equalsIgnoreCase(d.getImpactOnHandover()));

        Integer weeksRemaining = null;
        if (projectFinishDate != null) {
            int days = WorkingDayCalculator.workingDaysBetween(LocalDate.now(clock), projectFinishDate);
            weeksRemaining = (int) Math.round(days / 5.0);
        }

        return new ExpectedHandoverDto(projectFinishDate, baselineFinishDate, weeksRemaining, hasMaterialDelay);
    }
}
