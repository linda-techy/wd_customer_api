package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.ActivityFeedDto;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure-Mockito unit tests for {@link ActivityFeedService}. No Spring / DB.
 * Covers createActivity (happy + every null/exception branch), getProjectActivities,
 * getProjectActivitiesByDateRange, and the combined-feed grouping / type-filter paths.
 * The orphaned-author lazy-proxy path is already covered by ActivityFeedServiceCombinedFeedTest.
 */
@ExtendWith(MockitoExtension.class)
class ActivityFeedServiceUnitTest {

    @Mock private ActivityFeedRepository activityFeedRepository;
    @Mock private ActivityTypeRepository activityTypeRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CustomerUserRepository userRepository;
    @Mock private SiteReportRepository siteReportRepository;
    @Mock private ObservationRepository observationRepository;
    @Mock private QualityCheckRepository qualityCheckRepository;
    @Mock private GalleryImageRepository galleryImageRepository;
    @Mock private SiteVisitRepository siteVisitRepository;

    @InjectMocks private ActivityFeedService service;

    // ── createActivity ────────────────────────────────────────────────────────

    @Test
    void createActivity_happyPath_savesAndMapsDto() {
        ActivityType type = new ActivityType("DOCUMENT_UPLOADED", "doc", "#fff");
        type.setId(7L);
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(50L);
        CustomerUser user = mock(CustomerUser.class);
        when(user.getId()).thenReturn(3L);
        when(user.getFirstName()).thenReturn("Jane");
        when(user.getLastName()).thenReturn("Doe");

        when(activityTypeRepository.findByName("DOCUMENT_UPLOADED")).thenReturn(Optional.of(type));
        when(projectRepository.findById(50L)).thenReturn(Optional.of(project));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(activityFeedRepository.save(any(ActivityFeed.class)))
                .thenAnswer(inv -> {
                    ActivityFeed a = inv.getArgument(0);
                    a.setId(99L);
                    return a;
                });

        ActivityFeedDto dto = service.createActivity(50L, "DOCUMENT_UPLOADED", "Doc uploaded", 11L, 3L);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(99L);
        assertThat(dto.projectId()).isEqualTo(50L);
        assertThat(dto.activityTypeName()).isEqualTo("DOCUMENT_UPLOADED");
        assertThat(dto.activityTypeIcon()).isEqualTo("doc");
        assertThat(dto.activityTypeColor()).isEqualTo("#fff");
        assertThat(dto.title()).isEqualTo("Doc uploaded");
        assertThat(dto.referenceId()).isEqualTo(11L);
        assertThat(dto.createdById()).isEqualTo(3L);
        assertThat(dto.createdByName()).isEqualTo("Jane Doe");
        verify(activityFeedRepository).save(any(ActivityFeed.class));
    }

    @Test
    void createActivity_nullUser_mapsUnknownAuthorButStillSaves() {
        ActivityType type = new ActivityType("QC_ADDED", null, null);
        Project project = mock(Project.class);
        when(activityTypeRepository.findByName("QC_ADDED")).thenReturn(Optional.of(type));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(activityFeedRepository.save(any(ActivityFeed.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivityFeedDto dto = service.createActivity(1L, "QC_ADDED", "t", null, 999L);

        assertThat(dto).isNotNull();
        assertThat(dto.createdById()).isNull();
        assertThat(dto.createdByName()).isEqualTo("Unknown");
    }

    @Test
    void createActivity_unknownActivityType_returnsNullWithoutSaving() {
        when(activityTypeRepository.findByName("MISSING")).thenReturn(Optional.empty());
        when(projectRepository.findById(1L)).thenReturn(Optional.of(mock(Project.class)));

        ActivityFeedDto dto = service.createActivity(1L, "MISSING", "t", null, 3L);

        assertThat(dto).isNull();
        verify(activityFeedRepository, never()).save(any());
    }

    @Test
    void createActivity_unknownProject_returnsNullWithoutSaving() {
        when(activityTypeRepository.findByName("X")).thenReturn(Optional.of(new ActivityType("X", null, null)));
        when(projectRepository.findById(2L)).thenReturn(Optional.empty());

        ActivityFeedDto dto = service.createActivity(2L, "X", "t", null, 3L);

        assertThat(dto).isNull();
        verify(activityFeedRepository, never()).save(any());
    }

    @Test
    void createActivity_repositoryThrows_swallowsAndReturnsNull() {
        when(activityTypeRepository.findByName("X")).thenReturn(Optional.of(new ActivityType("X", null, null)));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(mock(Project.class)));
        when(userRepository.findById(3L)).thenReturn(Optional.empty());
        when(activityFeedRepository.save(any(ActivityFeed.class)))
                .thenThrow(new RuntimeException("db down"));

        ActivityFeedDto dto = service.createActivity(2L, "X", "t", null, 3L);

        assertThat(dto).isNull(); // must NOT propagate — best-effort side effect
    }

    // ── getProjectActivities / date range ─────────────────────────────────────

    @Test
    void getProjectActivities_mapsAllRows() {
        ActivityFeed a = activityFeed(1L, "TYPE_A");
        ActivityFeed b = activityFeed(2L, "TYPE_B");
        when(activityFeedRepository.findByProjectIdOrderByCreatedAtDesc(50L)).thenReturn(List.of(a, b));

        List<ActivityFeedDto> result = service.getProjectActivities(50L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ActivityFeedDto::activityTypeName)
                .containsExactly("TYPE_A", "TYPE_B");
    }

    @Test
    void getProjectActivities_empty_returnsEmptyList() {
        when(activityFeedRepository.findByProjectIdOrderByCreatedAtDesc(50L)).thenReturn(List.of());
        assertThat(service.getProjectActivities(50L)).isEmpty();
    }

    @Test
    void getProjectActivitiesByDateRange_passesRangeThroughAndMaps() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 31, 0, 0);
        when(activityFeedRepository.findByProjectIdAndCreatedAtBetweenOrderByCreatedAtDesc(50L, start, end))
                .thenReturn(List.of(activityFeed(5L, "T")));

        List<ActivityFeedDto> result = service.getProjectActivitiesByDateRange(50L, start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(5L);
        verify(activityFeedRepository)
                .findByProjectIdAndCreatedAtBetweenOrderByCreatedAtDesc(50L, start, end);
    }

    // ── combined feed: grouping, type filter, DRAFT exclusion ─────────────────

    @Test
    void getCombinedActivityFeed_excludesDraftSiteReports_sortsDescending() {
        SiteReport published = siteReport(1L, "PUBLISHED",
                LocalDateTime.of(2026, 5, 10, 9, 0));
        SiteReport draft = siteReport(2L, "DRAFT",
                LocalDateTime.of(2026, 5, 11, 9, 0));
        Observation obs = mock(Observation.class);
        when(obs.getId()).thenReturn(3L);
        when(obs.getReportedDate()).thenReturn(LocalDateTime.of(2026, 5, 12, 9, 0));

        when(siteReportRepository.findByProjectIdOrderByReportDateDesc(50L))
                .thenReturn(List.of(published, draft));
        when(observationRepository.findByProjectIdOrderByReportedDateDesc(50L))
                .thenReturn(List.of(obs));
        when(qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(50L)).thenReturn(List.of());
        when(galleryImageRepository.findByProjectIdOrderByTakenDateDesc(50L)).thenReturn(List.of());
        when(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(50L)).thenReturn(List.of());

        List<ActivityFeedService.CombinedActivityItem> items = service.getCombinedActivityFeed(50L);

        assertThat(items).hasSize(2); // draft excluded
        assertThat(items).extracting(ActivityFeedService.CombinedActivityItem::type)
                .containsExactly("OBSERVATION", "SITE_REPORT"); // newest first
    }

    @Test
    void getCombinedActivityFeedGroupedByDate_groupsByLocalDate() {
        SiteReport r1 = siteReport(1L, "PUBLISHED", LocalDateTime.of(2026, 5, 10, 9, 0));
        SiteReport r2 = siteReport(2L, "PUBLISHED", LocalDateTime.of(2026, 5, 10, 18, 0));
        SiteReport r3 = siteReport(3L, "PUBLISHED", LocalDateTime.of(2026, 5, 11, 8, 0));
        when(siteReportRepository.findByProjectIdOrderByReportDateDesc(50L))
                .thenReturn(List.of(r3, r2, r1));
        when(observationRepository.findByProjectIdOrderByReportedDateDesc(50L)).thenReturn(List.of());
        when(qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(50L)).thenReturn(List.of());
        when(galleryImageRepository.findByProjectIdOrderByTakenDateDesc(50L)).thenReturn(List.of());
        when(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(50L)).thenReturn(List.of());

        Map<LocalDate, List<ActivityFeedService.CombinedActivityItem>> grouped =
                service.getCombinedActivityFeedGroupedByDate(50L);

        assertThat(grouped).hasSize(2);
        assertThat(grouped.get(LocalDate.of(2026, 5, 10))).hasSize(2);
        assertThat(grouped.get(LocalDate.of(2026, 5, 11))).hasSize(1);
    }

    @Test
    void getCombinedActivityFeedByType_nullOrAll_returnsEverything() {
        stubFeedWithOneReport();
        assertThat(service.getCombinedActivityFeedByType(50L, null)).hasSize(1);
        stubFeedWithOneReport();
        assertThat(service.getCombinedActivityFeedByType(50L, "")).hasSize(1);
        stubFeedWithOneReport();
        assertThat(service.getCombinedActivityFeedByType(50L, "all")).hasSize(1);
    }

    @Test
    void getCombinedActivityFeedByType_specificType_filtersCaseInsensitively() {
        stubFeedWithOneReport();
        assertThat(service.getCombinedActivityFeedByType(50L, "site_report")).hasSize(1);
        stubFeedWithOneReport();
        assertThat(service.getCombinedActivityFeedByType(50L, "OBSERVATION")).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubFeedWithOneReport() {
        SiteReport report = siteReport(1L, "PUBLISHED", LocalDateTime.of(2026, 5, 10, 9, 0));
        when(siteReportRepository.findByProjectIdOrderByReportDateDesc(50L))
                .thenReturn(List.of(report));
        when(observationRepository.findByProjectIdOrderByReportedDateDesc(50L)).thenReturn(List.of());
        when(qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(50L)).thenReturn(List.of());
        when(galleryImageRepository.findByProjectIdOrderByTakenDateDesc(50L)).thenReturn(List.of());
        when(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(50L)).thenReturn(List.of());
    }

    private ActivityFeed activityFeed(Long id, String typeName) {
        ActivityFeed a = new ActivityFeed();
        a.setId(id);
        a.setTitle("title-" + id);
        a.setActivityType(new ActivityType(typeName, "icon", "color"));
        a.setCreatedAt(LocalDateTime.now());
        // createdBy + project left null → toDto exercises the null branches
        return a;
    }

    private SiteReport siteReport(Long id, String status, LocalDateTime reportDate) {
        SiteReport r = mock(SiteReport.class);
        // lenient: a DRAFT report is filtered out before toActivityItem(), so its
        // id/reportDate stubs go unused — must not trip strict-stubs.
        lenient().when(r.getId()).thenReturn(id);
        lenient().when(r.getStatus()).thenReturn(status);
        lenient().when(r.getReportDate()).thenReturn(reportDate);
        return r;
    }
}
