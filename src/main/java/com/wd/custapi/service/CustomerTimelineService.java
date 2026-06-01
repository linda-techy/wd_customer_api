package com.wd.custapi.service;

import com.wd.custapi.dto.TimelineItemDto;
import com.wd.custapi.dto.TimelineResponseDto;
import com.wd.custapi.dto.TimelineSummaryDto;
import com.wd.custapi.model.Task;
import com.wd.custapi.repository.CustomerTaskRepository;
import com.wd.custapi.repository.ProjectMilestoneRepository;
import com.wd.custapi.service.wbs.ProgressRollupService;
import com.wd.custapi.service.wbs.StatusLabelDeriver;
import com.wd.custapi.service.wbs.TimelineBucketingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CustomerTimelineService {

    private final CustomerTaskRepository taskRepo;
    private final ProjectMilestoneRepository milestoneRepo;
    private final TimelineBucketingService bucketing;
    private final StatusLabelDeriver labelDeriver;
    private final ProgressRollupService rollup;

    public CustomerTimelineService(CustomerTaskRepository taskRepo,
                                    ProjectMilestoneRepository milestoneRepo,
                                    TimelineBucketingService bucketing,
                                    StatusLabelDeriver labelDeriver,
                                    ProgressRollupService rollup) {
        this.taskRepo = taskRepo;
        this.milestoneRepo = milestoneRepo;
        this.bucketing = bucketing;
        this.labelDeriver = labelDeriver;
        this.rollup = rollup;
    }

    @Transactional(readOnly = true)
    public TimelineResponseDto getTimeline(Long projectId, String bucketName, int page, int size) {
        LocalDate today = LocalDate.now();
        TimelineBucketingService.WeekBounds w = bucketing.weekBounds(today);

        List<Task> tasks = switch (bucketName.toUpperCase()) {
            case "WEEK" -> taskRepo.findWeekBucket(projectId, w.start(), w.end());
            case "UPCOMING" -> taskRepo.findUpcomingBucket(projectId, w.end());
            case "COMPLETED" -> taskRepo.findCompletedBucket(projectId);
            default -> throw new IllegalArgumentException("Invalid bucket: " + bucketName);
        };

        int from = Math.min(page * size, tasks.size());
        int to = Math.min(from + size, tasks.size());
        List<Task> pageSlice = tasks.subList(from, to);

        TimelineResponseDto resp = new TimelineResponseDto();
        resp.setBucket(bucketName.toLowerCase());
        resp.setItems(pageSlice.stream().map(t -> toDto(t, today)).toList());
        resp.setTotalElements((long) tasks.size());
        resp.setTotalPages((int) Math.ceil((double) tasks.size() / size));
        resp.setPage(page);
        resp.setSize(size);
        resp.setProjectProgressPercent(computeProjectProgress(projectId));
        return resp;
    }

    @Transactional(readOnly = true)
    public TimelineSummaryDto getSummary(Long projectId) {
        LocalDate today = LocalDate.now();
        TimelineBucketingService.WeekBounds w = bucketing.weekBounds(today);

        TimelineSummaryDto s = new TimelineSummaryDto();
        s.setWeekCount(taskRepo.findWeekBucket(projectId, w.start(), w.end()).size());
        s.setUpcomingCount(taskRepo.findUpcomingBucket(projectId, w.end()).size());
        s.setCompletedCount(taskRepo.findCompletedBucket(projectId).size());
        s.setProjectProgressPercent(computeProjectProgress(projectId));
        return s;
    }

    private Integer computeProjectProgress(Long projectId) {
        var milestones = milestoneRepo.findByProjectIdOrderByIdAsc(projectId);
        var inputs = milestones.stream()
                .map(m -> new ProgressRollupService.MilestoneInput(
                        m.getCompletionPercentage() != null ? m.getCompletionPercentage() : BigDecimal.ZERO,
                        m.getWeightPercentage() != null ? m.getWeightPercentage() : BigDecimal.ONE,
                        m.getProgressSource() != null ? m.getProgressSource() : "COMPUTED"))
                .toList();
        return rollup.rollupProject(inputs).intValue();
    }

    private TimelineItemDto toDto(Task t, LocalDate today) {
        TimelineItemDto d = new TimelineItemDto();
        d.setTaskId(t.getId());
        d.setTitle(t.getTitle());
        d.setPlannedStart(t.getStartDate());
        d.setPlannedEnd(t.getEndDate());
        d.setActualEnd(t.getActualEndDate());
        d.setProgressPercent(t.getProgressPercent());
        d.setStatus(t.getStatus());
        d.setStatusLabel(labelDeriver.derive(t.getStartDate(), t.getEndDate(), today,
                t.getProgressPercent() != null ? t.getProgressPercent() : 0).name());
        if (t.getMilestoneId() != null) {
            d.setMilestoneId(t.getMilestoneId());
            milestoneRepo.findById(t.getMilestoneId()).ifPresent(m -> d.setMilestoneName(m.getName()));
        }
        return d;
    }
}
