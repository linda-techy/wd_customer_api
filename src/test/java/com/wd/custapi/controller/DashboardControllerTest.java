package com.wd.custapi.controller;

import com.wd.custapi.dto.DashboardDto;
import com.wd.custapi.dto.ProjectModuleDtos.ProjectPhaseDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.service.DashboardService;
import com.wd.custapi.service.ProjectPhaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link DashboardController}. Both services are
 * mocked and the controller is invoked directly. Tests cover each endpoint's
 * happy path, the {@code limit} bounds check on recent-projects, and the
 * shared {@code handleRuntimeException} mapping (NOT_FOUND / FORBIDDEN / 500)
 * which is reached via the project-detail and phase endpoints.
 */
@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock private DashboardService dashboardService;
    @Mock private ProjectPhaseService projectPhaseService;
    @Mock private Authentication auth;

    @InjectMocks
    private DashboardController controller;

    @BeforeEach
    void setUp() {
        lenient().when(auth.getName()).thenReturn("customer@example.com");
    }

    // ---- GET /api/dashboard ----

    @Test
    void getDashboard_success_returns200WithDto() {
        DashboardDto dto = mock(DashboardDto.class);
        when(dashboardService.getCustomerDashboard("customer@example.com")).thenReturn(dto);

        ResponseEntity<Object> r = controller.getDashboard(auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(dto);
    }

    @Test
    void getDashboard_serviceThrows_returns500() {
        when(dashboardService.getCustomerDashboard(anyString()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Object> r = controller.getDashboard(auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat((Map<String, Object>) r.getBody()).containsKey("error");
    }

    // ---- GET /recent-projects ----

    @Test
    void getRecentProjects_success_returnsList() {
        List<DashboardDto.ProjectCard> cards = List.of(mock(DashboardDto.ProjectCard.class));
        when(dashboardService.getRecentProjects("customer@example.com", 5)).thenReturn(cards);

        ResponseEntity<Object> r = controller.getRecentProjects(5, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(cards);
    }

    @Test
    void getRecentProjects_limitTooLow_returns400() {
        ResponseEntity<Object> r = controller.getRecentProjects(0, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat((Map<String, Object>) r.getBody()).containsEntry("error", "Limit must be between 1 and 50");
    }

    @Test
    void getRecentProjects_limitTooHigh_returns400() {
        ResponseEntity<Object> r = controller.getRecentProjects(51, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void getRecentProjects_serviceThrows_returns500() {
        when(dashboardService.getRecentProjects(anyString(), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Object> r = controller.getRecentProjects(5, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }

    // ---- GET /search-projects ----

    @Test
    void searchProjects_withQuery_returnsList() {
        List<DashboardDto.ProjectCard> cards = List.of(mock(DashboardDto.ProjectCard.class));
        when(dashboardService.searchProjects("customer@example.com", "villa")).thenReturn(cards);

        ResponseEntity<Object> r = controller.searchProjects("villa", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(cards);
    }

    @Test
    void searchProjects_nullQuery_passedThroughToService() {
        when(dashboardService.searchProjects(eq("customer@example.com"), isNull()))
                .thenReturn(List.of());

        ResponseEntity<Object> r = controller.searchProjects(null, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        verify(dashboardService).searchProjects("customer@example.com", null);
    }

    @Test
    void searchProjects_serviceThrows_returns500() {
        when(dashboardService.searchProjects(anyString(), any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Object> r = controller.searchProjects("x", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }

    // ---- GET /projects/{uuid} ----

    @Test
    void getProjectDetails_success_returns200() {
        DashboardDto.ProjectDetails details = mock(DashboardDto.ProjectDetails.class);
        when(dashboardService.getProjectDetails("uuid", "customer@example.com")).thenReturn(details);

        ResponseEntity<Object> r = controller.getProjectDetails("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(details);
    }

    @ParameterizedTest
    @CsvSource({
            "Project not found,                 404, Project not found",
            "Access denied for this project,    403, Access denied",
            "some other failure,                500, Operation failed: fetch project details"
    })
    void getProjectDetails_runtimeException_mapsStatusAndError(
            String thrownMessage, int expectedStatus, String expectedError) {
        when(dashboardService.getProjectDetails(anyString(), anyString()))
                .thenThrow(new RuntimeException(thrownMessage));

        ResponseEntity<Object> r = controller.getProjectDetails("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat((Map<String, Object>) r.getBody()).containsEntry("error", expectedError);
    }

    // ---- GET /projects/{uuid}/phases ----

    @Test
    void getProjectPhases_success_returnsPhaseList() {
        Project project = new Project();
        project.setId(7L);
        when(dashboardService.getProjectByUuidAndEmail("uuid", "customer@example.com"))
                .thenReturn(project);
        List<ProjectPhaseDto> phases = List.of(new ProjectPhaseDto(
                1L, "Foundation", "COMPLETED", 1, null, null, null, null));
        when(projectPhaseService.getProjectPhases(7L)).thenReturn(phases);

        ResponseEntity<Object> r = controller.getProjectPhases("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(phases);
    }

    @Test
    void getProjectPhases_notFound_returns404() {
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenThrow(new RuntimeException("Project not found"));

        ResponseEntity<Object> r = controller.getProjectPhases("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getProjectPhases_genericException_returns500() {
        Project project = new Project();
        project.setId(7L);
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenReturn(project);
        when(projectPhaseService.getProjectPhases(7L))
                .thenThrow(new IllegalStateException("phase calc broke"));

        ResponseEntity<Object> r = controller.getProjectPhases("uuid", auth);

        // IllegalStateException is a RuntimeException, msg has no "not found"/"access denied"
        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }

    // ---- PUT /projects/{uuid}/design-package ----

    @Test
    void updateDesignPackage_success_returns200() {
        DashboardDto.ProjectDetails details = mock(DashboardDto.ProjectDetails.class);
        when(dashboardService.updateDesignPackage("uuid", "premium", "customer@example.com"))
                .thenReturn(details);

        ResponseEntity<Object> r = controller.updateDesignPackage(
                "uuid",
                new DashboardController.DesignPackageUpdateRequest("premium"),
                auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(details);
    }

    @Test
    void updateDesignPackage_notFound_returns404() {
        when(dashboardService.updateDesignPackage(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Project not found"));

        ResponseEntity<Object> r = controller.updateDesignPackage(
                "uuid",
                new DashboardController.DesignPackageUpdateRequest("premium"),
                auth);

        assertThat(r.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void updateDesignPackage_genericException_returns500() {
        when(dashboardService.updateDesignPackage(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("io"));

        ResponseEntity<Object> r = controller.updateDesignPackage(
                "uuid",
                new DashboardController.DesignPackageUpdateRequest("premium"),
                auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }

    // ---- GET /admin/projects ----

    @Test
    void getAdminProjects_success_returns200WithPagedMap() {
        Map<String, Object> paged = Map.of("content", List.of(), "total", 0);
        when(dashboardService.getAdminProjectsPaged(0, 20, null)).thenReturn(paged);

        ResponseEntity<Map<String, Object>> r = controller.getAdminProjects(0, 20, null, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(paged);
    }

    @Test
    void getAdminProjects_serviceThrows_returns500() {
        when(dashboardService.getAdminProjectsPaged(anyInt(), anyInt(), any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> r = controller.getAdminProjects(0, 20, "q", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody()).containsEntry("error", "Failed to fetch admin projects");
    }
}
