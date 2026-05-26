package com.wd.custapi.service;

import com.wd.custapi.config.TestDataSeeder;
import com.wd.custapi.dto.ProjectModuleDtos.GalleryImageDto;
import com.wd.custapi.dto.ProjectModuleDtos.SiteVisitDto;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.GalleryImage;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.SiteVisit;
import com.wd.custapi.repository.GalleryImageRepository;
import com.wd.custapi.repository.SiteVisitRepository;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Regression lock for the {@code open-in-view: false} lazy-loading 500s.
 *
 * <p>The customer API releases its DB connection after each query (OSIV off).
 * Read services that build DTOs from entities therefore MUST run inside a
 * (read-only) transaction, or any lazy {@code @ManyToOne} touched during DTO
 * mapping throws {@link org.hibernate.LazyInitializationException} (or, when the
 * connection has already been recycled, a "This ResultSet is closed" PSQLException)
 * — surfacing to the customer app as a 500. This bit ActivityFeed, completed
 * SiteVisits, and Gallery (see audit Card 4.1 follow-up, 2026-05-26).
 *
 * <p>These tests deliberately run <b>without</b> {@code @Transactional}: a test
 * transaction would keep a session open across the whole method and mask the bug,
 * exactly as a mocked-repository unit test does. The data is committed in
 * {@link #seed()} (each {@code save} is its own transaction), so when the service
 * re-queries it gets fresh, uninitialised lazy proxies — the same conditions as a
 * real HTTP request.
 */
class CustomerReadOsivLazyLoadingIntegrationTest extends TestcontainersPostgresBase {

    @Autowired private TestDataSeeder seeder;
    @Autowired private ActivityFeedService activityFeedService;
    @Autowired private GalleryService galleryService;
    @Autowired private SiteVisitService siteVisitService;
    @Autowired private GalleryImageRepository galleryImageRepository;
    @Autowired private SiteVisitRepository siteVisitRepository;

    private Long projectId;
    private String expectedName;

    @BeforeEach
    void seed() {
        seeder.seed();
        Project project = seeder.getResidentialVilla();
        CustomerUser uploader = seeder.getCustomerA();
        projectId = project.getId();
        expectedName = uploader.getFirstName() + " " + uploader.getLastName(); // "Alice Anderson"

        // Isolation: the Testcontainers Postgres + Hibernate schema are reused across
        // test methods, and these inserts commit (this test is intentionally non-@Transactional),
        // so clear THIS project's probe rows before re-seeding exactly one of each.
        siteVisitRepository.deleteAll(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(projectId));
        galleryImageRepository.deleteAll(galleryImageRepository.findByProjectIdOrderByTakenDateDesc(projectId));

        // A gallery image whose uploadedBy is a real CustomerUser (lazy @ManyToOne).
        GalleryImage image = new GalleryImage();
        image.setProject(project);
        image.setUploadedBy(uploader);
        image.setImagePath("projects/" + projectId + "/gallery/osiv-probe.jpg");
        image.setCaption("OSIV probe photo");
        image.setTakenDate(LocalDate.now());
        image.setUploadedAt(LocalDateTime.now());
        galleryImageRepository.save(image);

        // A completed site visit whose visitor is a real CustomerUser (lazy @ManyToOne).
        SiteVisit visit = new SiteVisit();
        visit.setProject(project);
        visit.setVisitor(uploader);
        visit.setCheckInTime(LocalDateTime.now().minusHours(2));
        visit.setCheckOutTime(LocalDateTime.now().minusHours(1));
        visit.setPurpose("OSIV probe visit");
        siteVisitRepository.save(visit);
    }

    @Test
    void combinedActivityFeed_resolvesLazyAuthors_withoutLazyInitException() {
        Map<LocalDate, List<ActivityFeedService.CombinedActivityItem>> grouped =
                assertDoesNotThrow(() -> activityFeedService.getCombinedActivityFeedGroupedByDate(projectId));

        List<ActivityFeedService.CombinedActivityItem> items = grouped.values().stream()
                .flatMap(List::stream).toList();

        // Gallery + site-visit items must be present AND have their lazy author resolved
        // (proves the proxy was initialised in-session, not merely that nothing threw).
        assertThat(items).anyMatch(i -> "GALLERY".equals(i.type()) && expectedName.equals(i.createdByName()));
        assertThat(items).anyMatch(i -> "SITE_VISIT".equals(i.type()) && expectedName.equals(i.createdByName()));
    }

    @Test
    void getCompletedVisits_resolvesLazyVisitor_withoutLazyInitException() {
        List<SiteVisitDto> visits =
                assertDoesNotThrow(() -> siteVisitService.getCompletedVisits(projectId));

        assertThat(visits).hasSize(1);
        assertThat(visits.get(0).visitorName()).isEqualTo(expectedName);
    }

    @Test
    void getProjectImages_resolvesLazyUploader_withoutLazyInitException() {
        List<GalleryImageDto> images =
                assertDoesNotThrow(() -> galleryService.getProjectImages(projectId));

        assertThat(images).hasSize(1);
        assertThat(images.get(0).uploadedByName()).isEqualTo(expectedName);
    }
}
