package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Observation;
import com.wd.custapi.model.Observation.ObservationStatus;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.StaffRole;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.ObservationRepository;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.StaffRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservationServiceTest {

    @Mock private ObservationRepository observationRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CustomerUserRepository userRepository;
    @Mock private StaffRoleRepository staffRoleRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ActivityFeedService activityFeedService;
    @Mock private NotificationTriggerService notificationTriggerService;

    @InjectMocks private ObservationService service;

    private Project project;
    private CustomerUser user;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(10L);

        user = new CustomerUser();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
    }

    private Observation existingObservation(Long id, ObservationStatus status) {
        Observation obs = new Observation();
        obs.setId(id);
        obs.setProject(project);
        obs.setTitle("Crack in wall");
        obs.setDescription("desc");
        obs.setReportedBy(user);
        obs.setPriority(Observation.Priority.HIGH);
        obs.setStatus(status);
        return obs;
    }

    // ── createObservation ─────────────────────────────────────────────────────

    @Test
    void createObservation_happyPath_savesAndReturnsDto() {
        ObservationRequest request = new ObservationRequest(
                "Crack in wall", "Large crack near beam", null, "HIGH", "Living room");

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(observationRepository.save(any(Observation.class))).thenAnswer(inv -> {
            Observation o = inv.getArgument(0);
            o.setId(100L);
            return o;
        });

        ObservationDto dto = service.createObservation(10L, request, null, 1L);

        ArgumentCaptor<Observation> captor = ArgumentCaptor.forClass(Observation.class);
        verify(observationRepository).save(captor.capture());
        Observation saved = captor.getValue();
        assertEquals("Crack in wall", saved.getTitle());
        assertEquals(Observation.Priority.HIGH, saved.getPriority());
        assertEquals("Living room", saved.getLocation());
        assertEquals(user, saved.getReportedBy());
        assertEquals(project, saved.getProject());

        assertNotNull(dto);
        assertEquals(100L, dto.id());
        assertEquals(10L, dto.projectId());
        assertEquals("John Doe", dto.reportedByName());

        verify(activityFeedService).createActivity(eq(10L), eq("OBSERVATION_ADDED"),
                contains("Crack in wall"), eq(100L), eq(1L));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void createObservation_withRole_resolvesAndSetsRole() {
        ObservationRequest request = new ObservationRequest(
                "Title", "Desc", 7L, "MEDIUM", "Site");

        StaffRole role = new StaffRole();
        role.setId(7L);
        role.setName("Site Engineer");

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(staffRoleRepository.findById(7L)).thenReturn(Optional.of(role));
        when(observationRepository.save(any(Observation.class))).thenAnswer(inv -> inv.getArgument(0));

        ObservationDto dto = service.createObservation(10L, request, null, 1L);

        assertEquals(7L, dto.reportedByRoleId());
        assertEquals("Site Engineer", dto.reportedByRoleName());
    }

    @Test
    void createObservation_withImage_storesFileAndSetsImagePath() {
        ObservationRequest request = new ObservationRequest(
                "Title", "Desc", null, "LOW", null);
        MultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(fileStorageService.storeFile(eq(image), eq("projects/10/observations")))
                .thenReturn("projects/10/observations/photo.jpg");
        when(observationRepository.save(any(Observation.class))).thenAnswer(inv -> inv.getArgument(0));

        ObservationDto dto = service.createObservation(10L, request, image, 1L);

        assertEquals("projects/10/observations/photo.jpg", dto.imagePath());
        verify(fileStorageService).storeFile(image, "projects/10/observations");
    }

    @Test
    void createObservation_emptyImage_doesNotStoreFile() {
        ObservationRequest request = new ObservationRequest(
                "Title", "Desc", null, "LOW", null);
        MultipartFile image = new MockMultipartFile(
                "image", "empty.jpg", "image/jpeg", new byte[]{});

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(observationRepository.save(any(Observation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createObservation(10L, request, image, 1L);

        verify(fileStorageService, never()).storeFile(any(), anyString());
    }

    @Test
    void createObservation_projectNotFound_throws() {
        ObservationRequest request = new ObservationRequest(
                "Title", "Desc", null, "LOW", null);
        when(projectRepository.findById(10L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createObservation(10L, request, null, 1L));
        assertEquals("Project not found", ex.getMessage());
        verify(observationRepository, never()).save(any());
    }

    @Test
    void createObservation_userNotFound_throws() {
        ObservationRequest request = new ObservationRequest(
                "Title", "Desc", null, "LOW", null);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createObservation(10L, request, null, 1L));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void createObservation_roleNotFound_throws() {
        ObservationRequest request = new ObservationRequest(
                "Title", "Desc", 99L, "LOW", null);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(staffRoleRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createObservation(10L, request, null, 1L));
        assertEquals("Staff role not found", ex.getMessage());
    }

    // ── resolveObservation ────────────────────────────────────────────────────

    @Test
    void resolveObservation_happyPath_setsResolvedFieldsAndNotifies() {
        Observation obs = existingObservation(100L, ObservationStatus.OPEN);
        ObservationResolveRequest request = new ObservationResolveRequest("Fixed by re-plastering");

        when(observationRepository.findById(100L)).thenReturn(Optional.of(obs));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(observationRepository.save(any(Observation.class))).thenAnswer(inv -> inv.getArgument(0));

        ObservationDto dto = service.resolveObservation(10L, 100L, request, 1L);

        assertEquals("RESOLVED", dto.status());
        assertEquals("Fixed by re-plastering", dto.resolutionNotes());
        assertEquals(ObservationStatus.RESOLVED, obs.getStatus());
        assertNotNull(obs.getResolvedDate());
        assertEquals(user, obs.getResolvedBy());

        verify(activityFeedService).createActivity(eq(10L), eq("OBSERVATION_RESOLVED"),
                contains("Crack in wall"), eq(100L), eq(1L));
        verify(notificationTriggerService).notifyObservationResolved(
                eq(1L), eq(10L), eq(100L), eq("Crack in wall"));
    }

    @Test
    void resolveObservation_notFound_throws() {
        when(observationRepository.findById(100L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.resolveObservation(10L, 100L,
                        new ObservationResolveRequest("notes"), 1L));
        assertEquals("Observation not found", ex.getMessage());
    }

    @Test
    void resolveObservation_projectMismatch_throws() {
        Project otherProject = new Project();
        otherProject.setId(999L);
        Observation obs = existingObservation(100L, ObservationStatus.OPEN);
        obs.setProject(otherProject);

        when(observationRepository.findById(100L)).thenReturn(Optional.of(obs));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.resolveObservation(10L, 100L,
                        new ObservationResolveRequest("notes"), 1L));
        assertEquals("Observation not found", ex.getMessage());
        verify(observationRepository, never()).save(any());
    }

    @Test
    void resolveObservation_userNotFound_throws() {
        Observation obs = existingObservation(100L, ObservationStatus.OPEN);
        when(observationRepository.findById(100L)).thenReturn(Optional.of(obs));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.resolveObservation(10L, 100L,
                        new ObservationResolveRequest("notes"), 1L));
        assertEquals("User not found", ex.getMessage());
    }

    // ── getObservations ───────────────────────────────────────────────────────

    @Test
    void getObservations_withStatus_usesStatusQuery() {
        Observation obs = existingObservation(100L, ObservationStatus.OPEN);
        when(observationRepository
                .findByProjectIdAndStatusOrderByPriorityDescReportedDateDesc(10L, ObservationStatus.OPEN))
                .thenReturn(List.of(obs));

        List<ObservationDto> result = service.getObservations(10L, "OPEN");

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
        verify(observationRepository, never()).findByProjectIdOrderByReportedDateDesc(anyLong());
    }

    @Test
    void getObservations_nullStatus_usesAllQuery() {
        Observation obs = existingObservation(100L, ObservationStatus.IN_PROGRESS);
        when(observationRepository.findByProjectIdOrderByReportedDateDesc(10L))
                .thenReturn(List.of(obs));

        List<ObservationDto> result = service.getObservations(10L, null);

        assertEquals(1, result.size());
        assertEquals("IN_PROGRESS", result.get(0).status());
    }

    @Test
    void getObservations_invalidStatus_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getObservations(10L, "NOPE"));
    }

    @Test
    void getObservations_emptyResult_returnsEmptyList() {
        when(observationRepository.findByProjectIdOrderByReportedDateDesc(10L))
                .thenReturn(List.of());

        assertTrue(service.getObservations(10L, null).isEmpty());
    }

    // ── getActiveObservations ─────────────────────────────────────────────────

    @Test
    void getActiveObservations_queriesOpenAndInProgress() {
        Observation obs = existingObservation(100L, ObservationStatus.OPEN);
        when(observationRepository
                .findByProjectIdAndStatusInOrderByPriorityDescReportedDateDesc(
                        eq(10L), eq(List.of(ObservationStatus.OPEN, ObservationStatus.IN_PROGRESS))))
                .thenReturn(List.of(obs));

        List<ObservationDto> result = service.getActiveObservations(10L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).id());
    }

    // ── getResolvedObservations ───────────────────────────────────────────────

    @Test
    void getResolvedObservations_queriesResolvedStatus() {
        Observation obs = existingObservation(100L, ObservationStatus.RESOLVED);
        when(observationRepository
                .findByProjectIdAndStatusOrderByPriorityDescReportedDateDesc(10L, ObservationStatus.RESOLVED))
                .thenReturn(List.of(obs));

        List<ObservationDto> result = service.getResolvedObservations(10L);

        assertEquals(1, result.size());
        assertEquals("RESOLVED", result.get(0).status());
    }

    // ── getObservationCounts ──────────────────────────────────────────────────

    @Test
    void getObservationCounts_computesTotalsActiveResolved() {
        Observation open = existingObservation(1L, ObservationStatus.OPEN);
        Observation inProgress = existingObservation(2L, ObservationStatus.IN_PROGRESS);
        Observation resolved = existingObservation(3L, ObservationStatus.RESOLVED);
        when(observationRepository.findByProjectIdOrderByReportedDateDesc(10L))
                .thenReturn(List.of(open, inProgress, resolved));

        Map<String, Long> counts = service.getObservationCounts(10L);

        assertEquals(3L, counts.get("total"));
        assertEquals(2L, counts.get("active"));
        assertEquals(1L, counts.get("resolved"));
    }

    @Test
    void getObservationCounts_noObservations_allZero() {
        when(observationRepository.findByProjectIdOrderByReportedDateDesc(10L))
                .thenReturn(List.of());

        Map<String, Long> counts = service.getObservationCounts(10L);

        assertEquals(0L, counts.get("total"));
        assertEquals(0L, counts.get("active"));
        assertEquals(0L, counts.get("resolved"));
    }
}
