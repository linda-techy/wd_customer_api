package com.wd.custapi.service;

import com.wd.custapi.dto.CustomerSiteReportDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiteReportServiceTest {

    @Mock
    private SiteReportRepository siteReportRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private SiteReportService siteReportService;

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
        testReport.setReportDate(LocalDateTime.now());
    }

    @Test
    void getSiteReportById_WhenReportExists_ReturnsDto() {
        // Arrange
        doNothing().when(authorizationService).checkSiteReportAccess(anyString(), anyLong(), anyString());
        when(siteReportRepository.findById(100L)).thenReturn(Optional.of(testReport));

        // Act
        CustomerSiteReportDto result = siteReportService.getSiteReportById(testUserEmail, 100L);

        // Assert
        assertNotNull(result);
        assertEquals(testReport.getTitle(), result.getTitle());
        verify(authorizationService).checkSiteReportAccess(testUserEmail, 100L, "view");
        verify(siteReportRepository).findById(100L);
    }

    @Test
    void getSiteReportById_WhenReportNotFound_ThrowsResourceNotFoundException() {
        // Arrange
        doThrow(new ResourceNotFoundException("SiteReport", 999L))
            .when(authorizationService).checkSiteReportAccess(anyString(), anyLong(), anyString());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
            siteReportService.getSiteReportById(testUserEmail, 999L));
    }

    @Test
    void getSiteReportById_WhenUnauthorized_ThrowsUnauthorizedException() {
        // Arrange
        doThrow(new UnauthorizedException("view", "site report"))
            .when(authorizationService).checkSiteReportAccess(anyString(), anyLong(), anyString());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () ->
            siteReportService.getSiteReportById(testUserEmail, 100L));
    }

    @Test
    void getCustomerSiteReports_WithNoProjectFilter_ReturnsAllAccessibleReports() {
        // Arrange
        List<Long> accessibleProjectIds = Arrays.asList(1L, 2L, 3L);
        when(authorizationService.getAccessibleProjectIds(testUserEmail))
            .thenReturn(accessibleProjectIds);

        Pageable pageable = PageRequest.of(0, 20);
        Page<SiteReport> reportsPage = new PageImpl<>(Arrays.asList(testReport));
        when(siteReportRepository.findByProjectIdIn(accessibleProjectIds, pageable))
            .thenReturn(reportsPage);

        // Act
        Page<CustomerSiteReportDto> result = siteReportService.getCustomerSiteReports(
            testUserEmail, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
