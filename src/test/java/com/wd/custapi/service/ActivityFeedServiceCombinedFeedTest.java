package com.wd.custapi.service;

import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Guards the combined activity feed against an orphaned author FK: a portal/staff
 * user referenced by a site activity but absent from customer_users must NOT 500
 * the feed. Reproduces the live "Unable to find CustomerUser with id 30" 500.
 */
@ExtendWith(MockitoExtension.class)
class ActivityFeedServiceCombinedFeedTest {

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

    @Test
    void combinedFeed_tolerates_orphanedObservationAuthor() {
        Observation obs = mock(Observation.class);
        CustomerUser orphan = mock(CustomerUser.class);
        when(obs.getReportedBy()).thenReturn(orphan);
        // Lazy proxy whose backing row is gone -> property access throws.
        when(orphan.getFirstName()).thenThrow(new EntityNotFoundException(
                "Unable to find com.wd.custapi.model.CustomerUser with id 30"));

        when(siteReportRepository.findByProjectIdOrderByReportDateDesc(49L)).thenReturn(List.of());
        when(observationRepository.findByProjectIdOrderByReportedDateDesc(49L)).thenReturn(List.of(obs));
        when(qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(49L)).thenReturn(List.of());
        when(galleryImageRepository.findByProjectIdOrderByTakenDateDesc(49L)).thenReturn(List.of());
        when(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(49L)).thenReturn(List.of());

        assertThatCode(() -> {
            List<ActivityFeedService.CombinedActivityItem> items = service.getCombinedActivityFeed(49L);
            assertThat(items).hasSize(1);
            assertThat(items.get(0).type()).isEqualTo("OBSERVATION");
            assertThat(items.get(0).createdByName()).isEqualTo("Staff");
        }).doesNotThrowAnyException();
    }
}
