package com.wd.custapi.service;

import com.wd.custapi.dto.DashboardDto;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.Role;
import com.wd.custapi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private CustomerUserRepository customerUserRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectDocumentRepository projectDocumentRepository;

    @Mock
    private com.wd.custapi.repository.ProjectDesignStepRepository projectDesignStepRepository;

    @Mock
    private com.wd.custapi.repository.ActivityFeedRepository activityFeedRepository;

    @Mock
    private com.wd.custapi.repository.PaymentScheduleRepository paymentScheduleRepository;

    @Mock
    private com.wd.custapi.repository.ProjectMilestoneRepository projectMilestoneRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private CustomerUser customerUser;
    private Role customerRole;
    private Project project;

    @BeforeEach
    void setUp() {
        customerRole = new Role();
        customerRole.setName("CUSTOMER");

        customerUser = new CustomerUser();
        customerUser.setId(1L);
        customerUser.setEmail("john@example.com");
        customerUser.setFirstName("John");
        customerUser.setLastName("Doe");
        customerUser.setRole(customerRole);

        project = new Project();
        project.setId(10L);
        project.setProjectUuid(UUID.randomUUID());
        project.setName("Test Project");
        project.setCode("TP-001");
    }

    // ── getCustomerDashboard ──────────────────────────────────────────────────

    @Test
    void getCustomerDashboard_returnsUserInfoAndProjectCards() {
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(projectRepository.findAllByCustomerEmail("john@example.com")).thenReturn(List.of(project));
        when(projectDesignStepRepository.calculateDesignProgressBatch(anyList())).thenReturn(List.of());
        when(activityFeedRepository.findTop10ByProjectIdInOrderByCreatedAtDesc(anyList(), any()))
                .thenReturn(List.of());
        when(paymentScheduleRepository.getPaymentSummaryForProjects(anyList()))
                .thenReturn(new Object[]{1L, 0L, 1L, 50000.0, 0.0});

        DashboardDto result = dashboardService.getCustomerDashboard("john@example.com");

        assertNotNull(result);
        assertNotNull(result.getUser());
        assertEquals("john@example.com", result.getUser().getEmail());
        assertNotNull(result.getProjects());
        assertEquals(1L, result.getProjects().getTotalProjects());
    }

    @Test
    void getCustomerDashboard_unknownUser_throwsRuntimeException() {
        when(customerUserRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> dashboardService.getCustomerDashboard("unknown@example.com"));
    }

    // ── getRecentProjects ─────────────────────────────────────────────────────

    @Test
    void getRecentProjects_limitsToRequestedCount() {
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(projectRepository.findRecentByCustomerEmail("john@example.com", 3))
                .thenReturn(List.of(project));
        when(projectDesignStepRepository.calculateDesignProgressBatch(anyList())).thenReturn(List.of());

        List<DashboardDto.ProjectCard> cards = dashboardService.getRecentProjects("john@example.com", 3);

        assertNotNull(cards);
        assertEquals(1, cards.size());
        verify(projectRepository).findRecentByCustomerEmail("john@example.com", 3);
    }

    // ── searchProjects ────────────────────────────────────────────────────────

    @Test
    void searchProjects_withTerm_filtersProjects() {
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(projectRepository.searchByCustomerEmailAndTerm("john@example.com", "Test"))
                .thenReturn(List.of(project));
        when(projectDesignStepRepository.calculateDesignProgressBatch(anyList())).thenReturn(List.of());

        List<DashboardDto.ProjectCard> cards = dashboardService.searchProjects("john@example.com", "Test");

        assertNotNull(cards);
        assertEquals(1, cards.size());
        assertEquals("Test Project", cards.get(0).getName());
    }

    @Test
    void searchProjects_emptyTerm_fallsBackToRecentProjects() {
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(projectRepository.findRecentByCustomerEmail("john@example.com", 5)).thenReturn(List.of(project));
        when(projectDesignStepRepository.calculateDesignProgressBatch(anyList())).thenReturn(List.of());

        List<DashboardDto.ProjectCard> cards = dashboardService.searchProjects("john@example.com", "   ");

        assertNotNull(cards);
        verify(projectRepository, never()).searchByCustomerEmailAndTerm(any(), any());
        verify(projectRepository).findRecentByCustomerEmail("john@example.com", 5);
    }

    // ── getProjectDetails ─────────────────────────────────────────────────────

    @Test
    void getProjectDetails_validProjectForUser_returnsDetails() {
        UUID uuid = UUID.randomUUID();
        project.setProjectUuid(uuid);

        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(customerUser));
        when(projectRepository.findByProjectUuidAndCustomerEmail(uuid, "john@example.com")).thenReturn(project);
        when(projectDocumentRepository.findByReferenceIdAndReferenceTypeAndIsActiveTrue(10L, "PROJECT"))
                .thenReturn(List.of());
        when(projectDesignStepRepository.calculateDesignProgress(10L)).thenReturn(75.0);
        when(projectMilestoneRepository.findByProjectIdOrderByDueDateAsc(10L)).thenReturn(List.of());

        DashboardDto.ProjectDetails details = dashboardService.getProjectDetails(uuid.toString(), "john@example.com");

        assertNotNull(details);
        assertEquals("Test Project", details.getName());
        assertEquals(uuid.toString(), details.getProjectUuid());
        assertEquals(75.0, details.getDesignProgress());
    }

    @Test
    void getProjectDetails_unauthorizedUser_throwsRuntimeException() {
        UUID uuid = UUID.randomUUID();
        when(customerUserRepository.findByEmail("other@example.com")).thenReturn(Optional.of(customerUser));
        // non-admin user; project not accessible
        when(projectRepository.findByProjectUuidAndCustomerEmail(uuid, "other@example.com")).thenReturn(null);

        assertThrows(RuntimeException.class,
                () -> dashboardService.getProjectDetails(uuid.toString(), "other@example.com"));
    }
}
