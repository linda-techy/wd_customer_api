package com.wd.custapi.service;

import com.wd.custapi.dto.TimelineResponseDto;
import com.wd.custapi.dto.TimelineSummaryDto;
import com.wd.custapi.model.ProjectMilestone;
import com.wd.custapi.model.Task;
import com.wd.custapi.repository.CustomerTaskRepository;
import com.wd.custapi.repository.ProjectMilestoneRepository;
import com.wd.custapi.service.wbs.ProgressRollupService;
import com.wd.custapi.service.wbs.StatusLabelDeriver;
import com.wd.custapi.service.wbs.TimelineBucketingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link CustomerTimelineService}. Every constructor-injected
 * collaborator (two repositories + three wbs services) is mocked. Task entities
 * are read-only (no setters) so they are stubbed as Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class CustomerTimelineServiceTest {

    @Mock
    private CustomerTaskRepository taskRepo;

    @Mock
    private ProjectMilestoneRepository milestoneRepo;

    @Mock
    private TimelineBucketingService bucketing;

    @Mock
    private StatusLabelDeriver labelDeriver;

    @Mock
    private ProgressRollupService rollup;

    @InjectMocks
    private CustomerTimelineService service;

    private TimelineBucketingService.WeekBounds week;

    @BeforeEach
    void setUp() {
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        week = new TimelineBucketingService.WeekBounds(monday, monday.plusDays(6));
        // bucketing.weekBounds is called by every public method
        lenient().when(bucketing.weekBounds(any(LocalDate.class))).thenReturn(week);
        // default label so toDto never NPEs
        lenient().when(labelDeriver.derive(any(), any(), any(), anyInt()))
                .thenReturn(StatusLabelDeriver.Label.ON_TRACK);
    }

    private Task mockTask(Long id, String title, Integer progress, String status, Long milestoneId) {
        Task t = mock(Task.class);
        lenient().when(t.getId()).thenReturn(id);
        lenient().when(t.getTitle()).thenReturn(title);
        lenient().when(t.getStartDate()).thenReturn(LocalDate.now());
        lenient().when(t.getEndDate()).thenReturn(LocalDate.now().plusDays(3));
        lenient().when(t.getActualEndDate()).thenReturn(null);
        lenient().when(t.getProgressPercent()).thenReturn(progress);
        lenient().when(t.getStatus()).thenReturn(status);
        lenient().when(t.getMilestoneId()).thenReturn(milestoneId);
        return t;
    }

    private ProjectMilestone milestone(BigDecimal completion, BigDecimal weight, String source) {
        ProjectMilestone m = mock(ProjectMilestone.class);
        lenient().when(m.getCompletionPercentage()).thenReturn(completion);
        lenient().when(m.getWeightPercentage()).thenReturn(weight);
        lenient().when(m.getProgressSource()).thenReturn(source);
        return m;
    }

    // ===== getTimeline — bucket selection ================================

    @Test
    void getTimeline_weekBucket_queriesWeekRange() {
        Task t = mockTask(1L, "Footing", 30, "IN_PROGRESS", null);
        when(taskRepo.findWeekBucket(7L, week.start(), week.end())).thenReturn(List.of(t));
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(BigDecimal.ZERO);

        TimelineResponseDto resp = service.getTimeline(7L, "WEEK", 0, 10);

        assertEquals("week", resp.bucket);
        assertEquals(1, resp.items.size());
        assertEquals("Footing", resp.items.get(0).title);
        assertEquals(1L, resp.totalElements);
        assertEquals(1, resp.totalPages);
        verify(taskRepo).findWeekBucket(7L, week.start(), week.end());
    }

    @Test
    void getTimeline_upcomingBucket_queriesUpcoming() {
        when(taskRepo.findUpcomingBucket(7L, week.end())).thenReturn(List.of());
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(BigDecimal.ZERO);

        TimelineResponseDto resp = service.getTimeline(7L, "upcoming", 0, 10);

        assertEquals("upcoming", resp.bucket);
        assertTrue(resp.items.isEmpty());
        assertEquals(0L, resp.totalElements);
        verify(taskRepo).findUpcomingBucket(7L, week.end());
    }

    @Test
    void getTimeline_completedBucket_queriesCompleted() {
        when(taskRepo.findCompletedBucket(7L)).thenReturn(List.of());
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(BigDecimal.ZERO);

        TimelineResponseDto resp = service.getTimeline(7L, "completed", 0, 10);

        assertEquals("completed", resp.bucket);
        verify(taskRepo).findCompletedBucket(7L);
    }

    @Test
    void getTimeline_invalidBucket_throwsIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getTimeline(7L, "BOGUS", 0, 10));
        assertTrue(ex.getMessage().contains("BOGUS"));
        verifyNoInteractions(rollup);
    }

    // ===== getTimeline — pagination ======================================

    @Test
    void getTimeline_pagination_slicesSecondPage() {
        Task t1 = mockTask(1L, "T1", 10, "IN_PROGRESS", null);
        Task t2 = mockTask(2L, "T2", 20, "IN_PROGRESS", null);
        Task t3 = mockTask(3L, "T3", 30, "IN_PROGRESS", null);
        when(taskRepo.findWeekBucket(7L, week.start(), week.end()))
                .thenReturn(List.of(t1, t2, t3));
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(BigDecimal.ZERO);

        // page 1, size 2 -> [t3]
        TimelineResponseDto resp = service.getTimeline(7L, "WEEK", 1, 2);

        assertEquals(1, resp.items.size());
        assertEquals(3L, resp.items.get(0).taskId);
        assertEquals(3L, resp.totalElements);
        assertEquals(2, resp.totalPages);
        assertEquals(1, resp.page);
        assertEquals(2, resp.size);
    }

    @Test
    void getTimeline_pageBeyondData_returnsEmptySlice() {
        Task t1 = mockTask(1L, "T1", 10, "IN_PROGRESS", null);
        when(taskRepo.findWeekBucket(7L, week.start(), week.end())).thenReturn(List.of(t1));
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(BigDecimal.ZERO);

        TimelineResponseDto resp = service.getTimeline(7L, "WEEK", 5, 10);

        assertTrue(resp.items.isEmpty());
        assertEquals(1L, resp.totalElements);
    }

    // ===== getTimeline — toDto / milestone enrichment ====================

    @Test
    void getTimeline_taskWithMilestone_populatesMilestoneName() {
        Task t = mockTask(1L, "Slab", 50, "IN_PROGRESS", 42L);
        ProjectMilestone m = mock(ProjectMilestone.class);
        when(m.getName()).thenReturn("Foundation Done");

        when(taskRepo.findWeekBucket(7L, week.start(), week.end())).thenReturn(List.of(t));
        when(milestoneRepo.findById(42L)).thenReturn(Optional.of(m));
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(BigDecimal.ZERO);
        when(labelDeriver.derive(any(), any(), any(), anyInt()))
                .thenReturn(StatusLabelDeriver.Label.AT_RISK);

        TimelineResponseDto resp = service.getTimeline(7L, "WEEK", 0, 10);

        assertEquals(42L, resp.items.get(0).milestoneId);
        assertEquals("Foundation Done", resp.items.get(0).milestoneName);
        assertEquals("AT_RISK", resp.items.get(0).statusLabel);
    }

    @Test
    void getTimeline_taskWithMilestoneNotFound_leavesNameNull() {
        Task t = mockTask(1L, "Slab", 50, "IN_PROGRESS", 42L);
        when(taskRepo.findWeekBucket(7L, week.start(), week.end())).thenReturn(List.of(t));
        when(milestoneRepo.findById(42L)).thenReturn(Optional.empty());
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(BigDecimal.ZERO);

        TimelineResponseDto resp = service.getTimeline(7L, "WEEK", 0, 10);

        assertEquals(42L, resp.items.get(0).milestoneId);
        assertNull(resp.items.get(0).milestoneName);
    }

    @Test
    void getTimeline_nullProgressPercent_passesZeroToLabelDeriver() {
        Task t = mockTask(1L, "Slab", null, "IN_PROGRESS", null);
        when(taskRepo.findWeekBucket(7L, week.start(), week.end())).thenReturn(List.of(t));
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(BigDecimal.ZERO);

        service.getTimeline(7L, "WEEK", 0, 10);

        // null progress should be coerced to 0 in the derive(...) call
        verify(labelDeriver).derive(any(), any(), any(), eq(0));
    }

    // ===== getTimeline — project progress rollup =========================

    @Test
    void getTimeline_progressRollup_usesMilestoneInputsWithDefaults() {
        when(taskRepo.findWeekBucket(7L, week.start(), week.end())).thenReturn(List.of());
        // one fully-populated milestone, one with null fields -> defaults applied
        ProjectMilestone full = milestone(new BigDecimal("80"), new BigDecimal("2"), "MANUAL");
        ProjectMilestone defaults = milestone(null, null, null);
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of(full, defaults));
        when(rollup.rollupProject(any())).thenReturn(new BigDecimal("55.5"));

        TimelineResponseDto resp = service.getTimeline(7L, "WEEK", 0, 10);

        assertEquals(55, resp.projectProgressPercent, "intValue() truncates 55.5 -> 55");

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<ProgressRollupService.MilestoneInput>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(rollup).rollupProject(captor.capture());
        List<ProgressRollupService.MilestoneInput> inputs = captor.getValue();
        assertEquals(2, inputs.size());
        assertEquals(0, new BigDecimal("80").compareTo(inputs.get(0).effectiveProgress()));
        assertEquals("MANUAL", inputs.get(0).source());
        // defaults: completion -> ZERO, weight -> ONE, source -> COMPUTED
        assertEquals(0, BigDecimal.ZERO.compareTo(inputs.get(1).effectiveProgress()));
        assertEquals(0, BigDecimal.ONE.compareTo(inputs.get(1).weight()));
        assertEquals("COMPUTED", inputs.get(1).source());
    }

    // ===== getSummary ====================================================

    @Test
    void getSummary_countsEachBucketAndProgress() {
        // Build mock tasks first — calling mockTask() inside thenReturn() nests its
        // stubbing inside the outer when(...) and triggers UnfinishedStubbingException.
        Task t1 = mockTask(1L, "a", 10, "IN_PROGRESS", null);
        Task t2 = mockTask(2L, "b", 20, "IN_PROGRESS", null);
        Task t3 = mockTask(3L, "c", 0, "PENDING", null);
        Task t4 = mockTask(4L, "d", 100, "COMPLETED", null);
        Task t5 = mockTask(5L, "e", 100, "COMPLETED", null);
        Task t6 = mockTask(6L, "f", 100, "COMPLETED", null);
        when(taskRepo.findWeekBucket(7L, week.start(), week.end())).thenReturn(List.of(t1, t2));
        when(taskRepo.findUpcomingBucket(7L, week.end())).thenReturn(List.of(t3));
        when(taskRepo.findCompletedBucket(7L)).thenReturn(List.of(t4, t5, t6));
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(new BigDecimal("73.9"));

        TimelineSummaryDto s = service.getSummary(7L);

        assertEquals(2, s.weekCount);
        assertEquals(1, s.upcomingCount);
        assertEquals(3, s.completedCount);
        assertEquals(73, s.projectProgressPercent);
    }

    @Test
    void getSummary_noTasksNoMilestones_zeroEverywhere() {
        when(taskRepo.findWeekBucket(7L, week.start(), week.end())).thenReturn(List.of());
        when(taskRepo.findUpcomingBucket(7L, week.end())).thenReturn(List.of());
        when(taskRepo.findCompletedBucket(7L)).thenReturn(List.of());
        when(milestoneRepo.findByProjectIdOrderByIdAsc(7L)).thenReturn(List.of());
        when(rollup.rollupProject(any())).thenReturn(BigDecimal.ZERO);

        TimelineSummaryDto s = service.getSummary(7L);

        assertEquals(0, s.weekCount);
        assertEquals(0, s.upcomingCount);
        assertEquals(0, s.completedCount);
        assertEquals(0, s.projectProgressPercent);
    }
}
