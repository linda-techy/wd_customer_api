package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.SiteVisitCheckInRequest;
import com.wd.custapi.dto.ProjectModuleDtos.SiteVisitCheckOutRequest;
import com.wd.custapi.dto.ProjectModuleDtos.SiteVisitDto;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.SiteVisit;
import com.wd.custapi.model.StaffRole;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.SiteVisitRepository;
import com.wd.custapi.repository.StaffRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit test for {@link SiteVisitService}. All six collaborators are mocked;
 * {@link com.wd.custapi.util.GeoUtils} runs for real (static helper) so GPS
 * coordinates below are chosen to be either within or outside the 200 m geofence.
 */
@ExtendWith(MockitoExtension.class)
class SiteVisitServiceTest {

    @Mock private SiteVisitRepository siteVisitRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CustomerUserRepository userRepository;
    @Mock private StaffRoleRepository staffRoleRepository;
    @Mock private ActivityFeedService activityFeedService;
    @Mock private PortalUserLookup portalUserLookup;

    @InjectMocks private SiteVisitService service;

    // Project site reference point (Kochi, Kerala-ish)
    private static final double SITE_LAT = 10.000000;
    private static final double SITE_LON = 76.000000;
    // Same point → distance ~0 m, inside the 200 m geofence
    private static final double NEAR_LAT = 10.000000;
    private static final double NEAR_LON = 76.000000;
    // ~111 km north → far outside the geofence
    private static final double FAR_LAT = 11.000000;
    private static final double FAR_LON = 76.000000;

    private Project projectWithLocation;
    private Project projectNoLocation;
    private CustomerUser visitor;

    @BeforeEach
    void setUp() {
        projectWithLocation = new Project();
        projectWithLocation.setId(100L);
        projectWithLocation.setLatitude(SITE_LAT);
        projectWithLocation.setLongitude(SITE_LON);

        projectNoLocation = new Project();
        projectNoLocation.setId(101L);
        // no lat/lon → hasLocation() == false

        visitor = new CustomerUser();
        visitor.setId(7L);
        visitor.setFirstName("Anita");
        visitor.setLastName("Menon");
    }

    private SiteVisitCheckInRequest checkInReq(Double lat, Double lon, Long roleId, List<String> attendees) {
        return new SiteVisitCheckInRequest(roleId, "Inspection", "Site A", "Sunny", attendees, lat, lon);
    }

    private SiteVisitCheckOutRequest checkOutReq(Double lat, Double lon) {
        return new SiteVisitCheckOutRequest("Done", "All good", lat, lon);
    }

    // ────────────────────────────── checkIn ──────────────────────────────

    @Test
    void checkIn_happyPathWithinGeofence_savesVisitAndLogsActivity() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(projectWithLocation));
        when(userRepository.findById(7L)).thenReturn(Optional.of(visitor));
        when(siteVisitRepository
                .findTopByProjectIdAndVisitorIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(100L, 7L))
                .thenReturn(Optional.empty());
        StaffRole role = new StaffRole("Engineer");
        role.setId(3L);
        when(staffRoleRepository.findById(3L)).thenReturn(Optional.of(role));
        when(siteVisitRepository.save(any(SiteVisit.class))).thenAnswer(inv -> {
            SiteVisit v = inv.getArgument(0);
            v.setId(555L);
            return v;
        });

        SiteVisitDto dto = service.checkIn(100L, checkInReq(NEAR_LAT, NEAR_LON, 3L, List.of("Bob", "Carol")), 7L);

        ArgumentCaptor<SiteVisit> captor = ArgumentCaptor.forClass(SiteVisit.class);
        verify(siteVisitRepository).save(captor.capture());
        SiteVisit saved = captor.getValue();
        assertEquals(projectWithLocation, saved.getProject());
        assertEquals(visitor, saved.getVisitor());
        assertEquals("Inspection", saved.getPurpose());
        assertEquals("Site A", saved.getLocation());
        assertEquals("Sunny", saved.getWeatherConditions());
        assertEquals(NEAR_LAT, saved.getCheckInLatitude());
        assertNotNull(saved.getCheckInTime());
        assertEquals(role, saved.getVisitorRole());
        assertArrayEquals(new String[]{"Bob", "Carol"}, saved.getAttendees());
        // distance rounded to 3 decimals; near-zero
        assertNotNull(saved.getDistanceFromProjectCheckIn());
        assertTrue(saved.getDistanceFromProjectCheckIn() < 0.001);

        verify(activityFeedService).createActivity(100L, "SITE_VISIT_LOGGED",
                "Site visit started", 555L, 7L);

        // DTO mapping reflects the saved data
        assertEquals(555L, dto.id());
        assertEquals(100L, dto.projectId());
        assertEquals(7L, dto.visitorId());
        assertEquals("Anita Menon", dto.visitorName());
        assertEquals(3L, dto.visitorRoleId());
        assertEquals("Engineer", dto.visitorRoleName());
        assertEquals(List.of("Bob", "Carol"), dto.attendees());
    }

    @Test
    void checkIn_projectWithoutLocation_skipsGeofenceAndDistanceIsNull() {
        when(projectRepository.findById(101L)).thenReturn(Optional.of(projectNoLocation));
        when(userRepository.findById(7L)).thenReturn(Optional.of(visitor));
        when(siteVisitRepository
                .findTopByProjectIdAndVisitorIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(101L, 7L))
                .thenReturn(Optional.empty());
        when(siteVisitRepository.save(any(SiteVisit.class))).thenAnswer(inv -> {
            SiteVisit v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        // FAR coords but no project location → geofence skipped, still succeeds
        SiteVisitDto dto = service.checkIn(101L, checkInReq(FAR_LAT, FAR_LON, null, null), 7L);

        ArgumentCaptor<SiteVisit> captor = ArgumentCaptor.forClass(SiteVisit.class);
        verify(siteVisitRepository).save(captor.capture());
        assertNull(captor.getValue().getDistanceFromProjectCheckIn());
        assertNull(captor.getValue().getVisitorRole());
        assertNull(captor.getValue().getAttendees());
        assertNull(dto.visitorRoleId());
        assertNull(dto.attendees());
        verify(staffRoleRepository, never()).findById(any());
    }

    @Test
    void checkIn_projectNotFound_throws() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        SiteVisitCheckInRequest request = checkInReq(NEAR_LAT, NEAR_LON, null, null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkIn(999L, request, 7L));
        assertEquals("Project not found", ex.getMessage());
        verify(siteVisitRepository, never()).save(any());
    }

    @Test
    void checkIn_userNotFound_throws() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(projectWithLocation));
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        SiteVisitCheckInRequest request = checkInReq(NEAR_LAT, NEAR_LON, null, null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkIn(100L, request, 7L));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void checkIn_activeVisitExists_throws() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(projectWithLocation));
        when(userRepository.findById(7L)).thenReturn(Optional.of(visitor));
        when(siteVisitRepository
                .findTopByProjectIdAndVisitorIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(100L, 7L))
                .thenReturn(Optional.of(new SiteVisit()));

        SiteVisitCheckInRequest request = checkInReq(NEAR_LAT, NEAR_LON, null, null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkIn(100L, request, 7L));
        assertEquals("Please check out from your current visit first", ex.getMessage());
        verify(siteVisitRepository, never()).save(any());
    }

    @Test
    void checkIn_missingGpsCoordinates_throws() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(projectWithLocation));
        when(userRepository.findById(7L)).thenReturn(Optional.of(visitor));
        when(siteVisitRepository
                .findTopByProjectIdAndVisitorIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(100L, 7L))
                .thenReturn(Optional.empty());

        SiteVisitCheckInRequest request = checkInReq(null, null, null, null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkIn(100L, request, 7L));
        assertTrue(ex.getMessage().contains("GPS coordinates are required"));
        verify(siteVisitRepository, never()).save(any());
    }

    @Test
    void checkIn_outsideGeofence_throws() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(projectWithLocation));
        when(userRepository.findById(7L)).thenReturn(Optional.of(visitor));
        when(siteVisitRepository
                .findTopByProjectIdAndVisitorIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(100L, 7L))
                .thenReturn(Optional.empty());

        SiteVisitCheckInRequest request = checkInReq(FAR_LAT, FAR_LON, null, null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkIn(100L, request, 7L));
        assertTrue(ex.getMessage().startsWith("Check-in failed"));
        verify(siteVisitRepository, never()).save(any());
    }

    @Test
    void checkIn_staffRoleNotFound_throws() {
        when(projectRepository.findById(100L)).thenReturn(Optional.of(projectWithLocation));
        when(userRepository.findById(7L)).thenReturn(Optional.of(visitor));
        when(siteVisitRepository
                .findTopByProjectIdAndVisitorIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(100L, 7L))
                .thenReturn(Optional.empty());
        when(staffRoleRepository.findById(99L)).thenReturn(Optional.empty());

        SiteVisitCheckInRequest request = checkInReq(NEAR_LAT, NEAR_LON, 99L, null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkIn(100L, request, 7L));
        assertEquals("Staff role not found", ex.getMessage());
        verify(siteVisitRepository, never()).save(any());
    }

    // ────────────────────────────── checkOut ──────────────────────────────

    @Test
    void checkOut_happyPathWithinGeofence_setsCheckoutFieldsAndSaves() {
        SiteVisit visit = new SiteVisit();
        visit.setId(200L);
        visit.setProject(projectWithLocation);
        visit.setVisitor(visitor);
        when(siteVisitRepository.findById(200L)).thenReturn(Optional.of(visit));
        when(siteVisitRepository.save(any(SiteVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        SiteVisitDto dto = service.checkOut(200L, checkOutReq(NEAR_LAT, NEAR_LON));

        ArgumentCaptor<SiteVisit> captor = ArgumentCaptor.forClass(SiteVisit.class);
        verify(siteVisitRepository).save(captor.capture());
        SiteVisit saved = captor.getValue();
        assertNotNull(saved.getCheckOutTime());
        assertEquals("Done", saved.getNotes());
        assertEquals("All good", saved.getFindings());
        assertEquals(NEAR_LAT, saved.getCheckOutLatitude());
        assertEquals(NEAR_LON, saved.getCheckOutLongitude());
        assertNotNull(saved.getDistanceFromProjectCheckOut());
        assertEquals(200L, dto.id());
        assertNotNull(dto.checkOutTime());
    }

    @Test
    void checkOut_projectWithoutLocation_skipsGeofenceDistanceNull() {
        SiteVisit visit = new SiteVisit();
        visit.setId(201L);
        visit.setProject(projectNoLocation);
        visit.setVisitor(visitor);
        when(siteVisitRepository.findById(201L)).thenReturn(Optional.of(visit));
        when(siteVisitRepository.save(any(SiteVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        service.checkOut(201L, checkOutReq(FAR_LAT, FAR_LON)); // far, but no project loc

        ArgumentCaptor<SiteVisit> captor = ArgumentCaptor.forClass(SiteVisit.class);
        verify(siteVisitRepository).save(captor.capture());
        assertNull(captor.getValue().getDistanceFromProjectCheckOut());
    }

    @Test
    void checkOut_visitNotFound_throws() {
        when(siteVisitRepository.findById(404L)).thenReturn(Optional.empty());

        SiteVisitCheckOutRequest request = checkOutReq(NEAR_LAT, NEAR_LON);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkOut(404L, request));
        assertEquals("Site visit not found", ex.getMessage());
    }

    @Test
    void checkOut_alreadyCheckedOut_throws() {
        SiteVisit visit = new SiteVisit();
        visit.setId(202L);
        visit.setProject(projectWithLocation);
        visit.setCheckOutTime(java.time.LocalDateTime.now());
        when(siteVisitRepository.findById(202L)).thenReturn(Optional.of(visit));

        SiteVisitCheckOutRequest request = checkOutReq(NEAR_LAT, NEAR_LON);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkOut(202L, request));
        assertEquals("Already checked out", ex.getMessage());
        verify(siteVisitRepository, never()).save(any());
    }

    @Test
    void checkOut_missingGpsCoordinates_throws() {
        SiteVisit visit = new SiteVisit();
        visit.setId(203L);
        visit.setProject(projectWithLocation);
        when(siteVisitRepository.findById(203L)).thenReturn(Optional.of(visit));

        SiteVisitCheckOutRequest request = checkOutReq(null, null);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkOut(203L, request));
        assertTrue(ex.getMessage().contains("GPS coordinates are required"));
        verify(siteVisitRepository, never()).save(any());
    }

    @Test
    void checkOut_outsideGeofence_throws() {
        SiteVisit visit = new SiteVisit();
        visit.setId(204L);
        visit.setProject(projectWithLocation);
        when(siteVisitRepository.findById(204L)).thenReturn(Optional.of(visit));

        SiteVisitCheckOutRequest request = checkOutReq(FAR_LAT, FAR_LON);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.checkOut(204L, request));
        assertTrue(ex.getMessage().startsWith("Check-out failed"));
        verify(siteVisitRepository, never()).save(any());
    }

    // ─────────────────── list endpoints + toDto branches ───────────────────

    private SiteVisit customerVisit(long id) {
        SiteVisit v = new SiteVisit();
        v.setId(id);
        v.setProject(projectWithLocation);
        v.setVisitor(visitor);
        return v;
    }

    @Test
    void getProjectVisits_mapsAllRows() {
        when(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(100L))
                .thenReturn(List.of(customerVisit(1L), customerVisit(2L)));

        List<SiteVisitDto> result = service.getProjectVisits(100L);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Anita Menon", result.get(0).visitorName());
    }

    @Test
    void getProjectVisits_emptyList_returnsEmpty() {
        when(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(100L)).thenReturn(List.of());
        assertTrue(service.getProjectVisits(100L).isEmpty());
    }

    @Test
    void getCompletedVisits_delegatesToCompletedQuery() {
        when(siteVisitRepository.findByProjectIdAndCheckOutTimeIsNotNullOrderByCheckInTimeDesc(100L))
                .thenReturn(List.of(customerVisit(9L)));

        List<SiteVisitDto> result = service.getCompletedVisits(100L);

        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).id());
        verify(siteVisitRepository).findByProjectIdAndCheckOutTimeIsNotNullOrderByCheckInTimeDesc(100L);
    }

    @Test
    void getOngoingVisits_delegatesToOngoingQuery() {
        when(siteVisitRepository.findByProjectIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(100L))
                .thenReturn(List.of(customerVisit(11L)));

        List<SiteVisitDto> result = service.getOngoingVisits(100L);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).id());
        verify(siteVisitRepository).findByProjectIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(100L);
    }

    @Test
    void toDto_staffSideRow_resolvesNameViaPortalLookupAndHumanisesVisitType() {
        SiteVisit staffVisit = new SiteVisit();
        staffVisit.setId(300L);
        staffVisit.setProject(projectWithLocation);
        // visitor null; visitedBy + visitType are read-only mirror columns (no setters)
        ReflectionTestUtils.setField(staffVisit, "visitedBy", 42L);
        ReflectionTestUtils.setField(staffVisit, "visitType", "SITE_ENGINEER");

        when(portalUserLookup.lookup(42L))
                .thenReturn(new PortalUserLookup.View(42L, "Ravi Kumar", null, null, null));
        when(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(100L))
                .thenReturn(List.of(staffVisit));

        SiteVisitDto dto = service.getProjectVisits(100L).get(0);

        assertEquals(42L, dto.visitorId());
        assertEquals("Ravi Kumar", dto.visitorName());
        assertNull(dto.visitorRoleId());
        assertEquals("Site Engineer", dto.visitorRoleName());
    }

    @Test
    void toDto_staffSideRow_lookupMisses_fallsBackToStaffLabel() {
        SiteVisit staffVisit = new SiteVisit();
        staffVisit.setId(301L);
        staffVisit.setProject(projectWithLocation);
        ReflectionTestUtils.setField(staffVisit, "visitedBy", 43L);
        ReflectionTestUtils.setField(staffVisit, "visitType", "PROJECT_MANAGER");

        when(portalUserLookup.lookup(43L)).thenReturn(null);
        when(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(100L))
                .thenReturn(List.of(staffVisit));

        SiteVisitDto dto = service.getProjectVisits(100L).get(0);

        assertEquals("Staff", dto.visitorName());
        assertEquals("Project Manager", dto.visitorRoleName());
    }

    @Test
    void toDto_bothVisitorPointersNull_usesDefensivePlaceholders() {
        SiteVisit orphan = new SiteVisit();
        orphan.setId(302L);
        orphan.setProject(projectWithLocation);
        // visitor null, visitedBy null, visitType null
        when(siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(100L))
                .thenReturn(List.of(orphan));

        SiteVisitDto dto = service.getProjectVisits(100L).get(0);

        assertEquals(0L, dto.visitorId());
        assertEquals("Staff", dto.visitorName());
        assertNull(dto.visitorRoleId());
        assertNull(dto.visitorRoleName());
        verifyNoInteractions(portalUserLookup);
    }
}
