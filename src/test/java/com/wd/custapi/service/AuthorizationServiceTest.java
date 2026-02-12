package com.wd.custapi.service;

import com.wd.custapi.exception.ResourceNotFoundException;
import com.wd.custapi.exception.UnauthorizedException;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.SiteReport;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.SiteReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private SiteReportRepository siteReportRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    private Project testProject;
    private SiteReport testReport;
    private String testUserEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("Test Project");

        testReport = new SiteReport();
        testReport.setId(100L);
        testReport.setTitle("Test Report");
        testReport.setProject(testProject);
    }

    @Test
    void checkSiteReportAccess_WhenAuthorized_DoesNotThrowException() {
        // Arrange
        when(siteReportRepository.findById(100L))
            .thenReturn(Optional.of(testReport));
        when(dashboardService.getProjectsForUser(testUserEmail))
            .thenReturn(Arrays.asList(testProject));

        // Act & Assert
        assertDoesNotThrow(() ->
            authorizationService.checkSiteReportAccess(testUserEmail, 100L, "view"));
        
        verify(siteReportRepository).findById(100L);
        verify(dashboardService).getProjectsForUser(testUserEmail);
    }

    @Test
    void checkSiteReportAccess_WhenReportNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(siteReportRepository.findById(999L))
            .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            authorizationService.checkSiteReportAccess(testUserEmail, 999L, "view"));
        
        assertEquals("SiteReport", exception.getResourceType());
        assertEquals(999L, exception.getResourceId());
        verify(siteReportRepository).findById(999L);
        verify(dashboardService, never()).getProjectsForUser(anyString());
    }

    @Test
    void checkSiteReportAccess_WhenReportHasNullProject_ThrowsResourceNotFoundException() {
        // Arrange
        testReport.setProject(null);
        when(siteReportRepository.findById(100L))
            .thenReturn(Optional.of(testReport));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            authorizationService.checkSiteReportAccess(testUserEmail, 100L, "view"));
        
        assertEquals("SiteReport", exception.getResourceType());
        verify(siteReportRepository).findById(100L);
        verify(dashboardService, never()).getProjectsForUser(anyString());
    }

    @Test
    void checkSiteReportAccess_WhenUnauthorized_ThrowsUnauthorizedException() {
        // Arrange
        Project differentProject = new Project();
        differentProject.setId(999L);
        
        when(siteReportRepository.findById(100L))
            .thenReturn(Optional.of(testReport));
        when(dashboardService.getProjectsForUser(testUserEmail))
            .thenReturn(Arrays.asList(differentProject));

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            authorizationService.checkSiteReportAccess(testUserEmail, 100L, "view"));
        
        assertEquals("view", exception.getAction());
        assertEquals("site report", exception.getResource());
        verify(siteReportRepository).findById(100L);
        verify(dashboardService).getProjectsForUser(testUserEmail);
    }

    @Test
    void getAccessibleProjectIds_ReturnsProjectIdList() {
        // Arrange
        Project project1 = new Project();
        project1.setId(1L);
        Project project2 = new Project();
        project2.setId(2L);
        
        when(dashboardService.getProjectsForUser(testUserEmail))
            .thenReturn(Arrays.asList(project1, project2));

        // Act
        List<Long> result = authorizationService.getAccessibleProjectIds(testUserEmail);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
        verify(dashboardService).getProjectsForUser(testUserEmail);
    }

    @Test
    void checkProjectAccess_WhenAuthorized_DoesNotThrowException() {
        // Arrange
        when(projectRepository.existsById(1L))
            .thenReturn(true);
        when(dashboardService.getProjectsForUser(testUserEmail))
            .thenReturn(Arrays.asList(testProject));

        // Act & Assert
        assertDoesNotThrow(() ->
            authorizationService.checkProjectAccess(testUserEmail, 1L, "view"));
        
        verify(projectRepository).existsById(1L);
        verify(dashboardService).getProjectsForUser(testUserEmail);
    }

    @Test
    void checkProjectAccess_WhenProjectNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(projectRepository.existsById(999L))
            .thenReturn(false);

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            authorizationService.checkProjectAccess(testUserEmail, 999L, "view"));
        
        assertEquals("Project", exception.getResourceType());
        assertEquals(999L, exception.getResourceId());
        verify(projectRepository).existsById(999L);
        verify(dashboardService, never()).getProjectsForUser(anyString());
    }

    @Test
    void checkProjectAccess_WhenUnauthorized_ThrowsUnauthorizedException() {
        // Arrange
        when(projectRepository.existsById(1L))
            .thenReturn(true);
        when(dashboardService.getProjectsForUser(testUserEmail))
            .thenReturn(List.of()); // No projects

        // Act & Assert
        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
            authorizationService.checkProjectAccess(testUserEmail, 1L, "view"));
        
        assertEquals("view", exception.getAction());
        assertEquals("project", exception.getResource());
        verify(projectRepository).existsById(1L);
        verify(dashboardService).getProjectsForUser(testUserEmail);
    }
}
