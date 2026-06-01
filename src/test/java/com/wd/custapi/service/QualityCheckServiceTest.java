package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.QualityCheck;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.QualityCheckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QualityCheckServiceTest {

    @Mock private QualityCheckRepository qualityCheckRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CustomerUserRepository userRepository;
    @Mock private ActivityFeedService activityFeedService;

    @InjectMocks private QualityCheckService service;

    private Project project;
    private CustomerUser user;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(10L);

        user = new CustomerUser();
        user.setId(1L);
        user.setFirstName("Jane");
        user.setLastName("Smith");
    }

    private QualityCheck existingQc(Long id, QualityCheck.QualityCheckStatus status) {
        QualityCheck qc = new QualityCheck();
        qc.setId(id);
        qc.setProject(project);
        qc.setTitle("Concrete slump test");
        qc.setDescription("desc");
        qc.setPriority(QualityCheck.Priority.HIGH);
        qc.setStatus(status);
        qc.setCreatedBy(user);
        return qc;
    }

    // ── createQualityCheck ────────────────────────────────────────────────────

    @Test
    void createQualityCheck_happyPath_savesAndReturnsDto() {
        QualityCheckRequest request = new QualityCheckRequest(
                "Concrete slump test", "Check slump", "SOP-12", "HIGH", null);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(qualityCheckRepository.save(any(QualityCheck.class))).thenAnswer(inv -> {
            QualityCheck qc = inv.getArgument(0);
            qc.setId(200L);
            return qc;
        });

        QualityCheckDto dto = service.createQualityCheck(10L, request, 1L);

        ArgumentCaptor<QualityCheck> captor = ArgumentCaptor.forClass(QualityCheck.class);
        verify(qualityCheckRepository).save(captor.capture());
        QualityCheck saved = captor.getValue();
        assertEquals("Concrete slump test", saved.getTitle());
        assertEquals("SOP-12", saved.getSopReference());
        assertEquals(QualityCheck.Priority.HIGH, saved.getPriority());
        assertEquals(user, saved.getCreatedBy());
        assertNull(saved.getAssignedTo());

        assertEquals(200L, dto.id());
        assertEquals(10L, dto.projectId());
        assertEquals("Jane Smith", dto.createdByName());
        assertEquals(1L, dto.createdById());

        verify(activityFeedService).createActivity(eq(10L), eq("QUALITY_CHECK_ADDED"),
                contains("Concrete slump test"), eq(200L), eq(1L));
    }

    @Test
    void createQualityCheck_withAssignee_resolvesAndSetsAssignedTo() {
        CustomerUser assignee = new CustomerUser();
        assignee.setId(5L);
        assignee.setFirstName("Bob");
        assignee.setLastName("Lee");

        QualityCheckRequest request = new QualityCheckRequest(
                "Title", "Desc", null, "MEDIUM", 5L);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(5L)).thenReturn(Optional.of(assignee));
        when(qualityCheckRepository.save(any(QualityCheck.class))).thenAnswer(inv -> inv.getArgument(0));

        QualityCheckDto dto = service.createQualityCheck(10L, request, 1L);

        assertEquals(5L, dto.assignedToId());
        assertEquals("Bob Lee", dto.assignedToName());
    }

    @Test
    void createQualityCheck_projectNotFound_throws() {
        QualityCheckRequest request = new QualityCheckRequest(
                "Title", "Desc", null, "LOW", null);
        when(projectRepository.findById(10L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createQualityCheck(10L, request, 1L));
        assertEquals("Project not found", ex.getMessage());
        verify(qualityCheckRepository, never()).save(any());
    }

    @Test
    void createQualityCheck_userNotFound_throws() {
        QualityCheckRequest request = new QualityCheckRequest(
                "Title", "Desc", null, "LOW", null);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createQualityCheck(10L, request, 1L));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void createQualityCheck_assigneeNotFound_throws() {
        QualityCheckRequest request = new QualityCheckRequest(
                "Title", "Desc", null, "LOW", 99L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createQualityCheck(10L, request, 1L));
        assertEquals("Assigned user not found", ex.getMessage());
    }

    // ── resolveQualityCheck ───────────────────────────────────────────────────

    @Test
    void resolveQualityCheck_happyPath_setsResolvedFields() {
        QualityCheck qc = existingQc(200L, QualityCheck.QualityCheckStatus.ACTIVE);
        QualityCheckUpdateRequest request = new QualityCheckUpdateRequest("Passed re-test");

        when(qualityCheckRepository.findById(200L)).thenReturn(Optional.of(qc));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(qualityCheckRepository.save(any(QualityCheck.class))).thenAnswer(inv -> inv.getArgument(0));

        QualityCheckDto dto = service.resolveQualityCheck(200L, request, 1L);

        assertEquals("RESOLVED", dto.status());
        assertEquals("Passed re-test", dto.resolutionNotes());
        assertEquals(QualityCheck.QualityCheckStatus.RESOLVED, qc.getStatus());
        assertNotNull(qc.getResolvedAt());
        assertEquals(user, qc.getResolvedBy());

        verify(activityFeedService).createActivity(eq(10L), eq("QUALITY_CHECK_RESOLVED"),
                contains("Concrete slump test"), eq(200L), eq(1L));
    }

    @Test
    void resolveQualityCheck_notFound_throws() {
        when(qualityCheckRepository.findById(200L)).thenReturn(Optional.empty());

        QualityCheckUpdateRequest request = new QualityCheckUpdateRequest("notes");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.resolveQualityCheck(200L, request, 1L));
        assertEquals("Quality check not found", ex.getMessage());
    }

    @Test
    void resolveQualityCheck_userNotFound_throws() {
        QualityCheck qc = existingQc(200L, QualityCheck.QualityCheckStatus.ACTIVE);
        when(qualityCheckRepository.findById(200L)).thenReturn(Optional.of(qc));
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        QualityCheckUpdateRequest request = new QualityCheckUpdateRequest("notes");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.resolveQualityCheck(200L, request, 1L));
        assertEquals("User not found", ex.getMessage());
        verify(qualityCheckRepository, never()).save(any());
    }

    // ── getQualityChecks ──────────────────────────────────────────────────────

    @Test
    void getQualityChecks_withStatus_usesStatusQuery() {
        QualityCheck qc = existingQc(200L, QualityCheck.QualityCheckStatus.ACTIVE);
        when(qualityCheckRepository
                .findByProjectIdAndStatusOrderByPriorityDescCreatedAtDesc(
                        10L, QualityCheck.QualityCheckStatus.ACTIVE))
                .thenReturn(List.of(qc));

        List<QualityCheckDto> result = service.getQualityChecks(10L, "ACTIVE");

        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).id());
        verify(qualityCheckRepository, never()).findByProjectIdOrderByCreatedAtDesc(anyLong());
    }

    @Test
    void getQualityChecks_nullStatus_usesAllQuery() {
        QualityCheck qc = existingQc(200L, QualityCheck.QualityCheckStatus.RESOLVED);
        when(qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(qc));

        List<QualityCheckDto> result = service.getQualityChecks(10L, null);

        assertEquals(1, result.size());
        assertEquals("RESOLVED", result.get(0).status());
    }

    @Test
    void getQualityChecks_invalidStatus_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getQualityChecks(10L, "BOGUS"));
    }

    @Test
    void getQualityChecks_emptyResult_returnsEmptyList() {
        when(qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of());

        assertTrue(service.getQualityChecks(10L, null).isEmpty());
    }

    // ── toDto defensive branches (exercised via getQualityChecks) ─────────────

    @Test
    void toDto_nullCreatedBy_fallsBackToSiteEngineer() {
        QualityCheck qc = existingQc(200L, QualityCheck.QualityCheckStatus.ACTIVE);
        qc.setCreatedBy(null);
        when(qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(qc));

        QualityCheckDto dto = service.getQualityChecks(10L, null).get(0);

        assertNull(dto.createdById());
        assertEquals("Site Engineer", dto.createdByName());
    }

    @Test
    void toDto_danglingCreatedByProxy_fallsBackToSiteEngineer() {
        QualityCheck qc = existingQc(200L, QualityCheck.QualityCheckStatus.ACTIVE);
        CustomerUser dangling = mock(CustomerUser.class);
        when(dangling.getId()).thenThrow(new RuntimeException("LazyInitializationException"));
        qc.setCreatedBy(dangling);
        when(qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(qc));

        QualityCheckDto dto = service.getQualityChecks(10L, null).get(0);

        assertNull(dto.createdById());
        assertEquals("Site Engineer", dto.createdByName());
    }

    @Test
    void toDto_nullStatusAndPriority_useDefaults() {
        QualityCheck qc = existingQc(200L, QualityCheck.QualityCheckStatus.ACTIVE);
        qc.setStatus(null);
        qc.setPriority(null);
        when(qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(qc));

        QualityCheckDto dto = service.getQualityChecks(10L, null).get(0);

        assertEquals("ACTIVE", dto.status());
        assertEquals("MEDIUM", dto.priority());
    }
}
