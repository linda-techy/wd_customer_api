package com.wd.custapi.service;

import com.wd.custapi.exception.ResourceNotFoundException;
import com.wd.custapi.exception.UnauthorizedException;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.SiteReport;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.SiteReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralized authorization service for checking user permissions.
 * Handles all authorization logic to ensure consistent security enforcement.
 */
@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    private final DashboardService dashboardService;
    private final SiteReportRepository siteReportRepository;
    private final ProjectRepository projectRepository;

    public AuthorizationService(DashboardService dashboardService,
                                SiteReportRepository siteReportRepository,
                                ProjectRepository projectRepository) {
        this.dashboardService = dashboardService;
        this.siteReportRepository = siteReportRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Check if user has access to a specific site report.
     * Throws exception if report doesn't exist or user lacks access.
     *
     * @param userEmail User's email
     * @param reportId Report ID to check
     * @param action Action being performed (for logging)
     * @throws ResourceNotFoundException if report doesn't exist
     * @throws UnauthorizedException if user lacks access
     */
    @Transactional(readOnly = true)
    public void checkSiteReportAccess(String userEmail, Long reportId, String action) {
        logger.debug("Checking site report access for user {} on report {}", userEmail, reportId);

        // Fetch report
        SiteReport report = siteReportRepository.findById(reportId)
            .orElseThrow(() -> {
                logger.warn("Site report not found: {}", reportId);
                return new ResourceNotFoundException("SiteReport", reportId);
            });

        // Validate project relationship
        if (report.getProject() == null) {
            logger.error("Site report {} has null project reference", reportId);
            throw new ResourceNotFoundException("SiteReport", reportId);
        }

        // Check project access
        List<Project> userProjects = dashboardService.getProjectsForUser(userEmail);
        boolean hasAccess = userProjects.stream()
            .anyMatch(p -> p.getId().equals(report.getProject().getId()));

        if (!hasAccess) {
            logger.warn("User {} unauthorized to {} site report {}", userEmail, action, reportId);
            throw new UnauthorizedException(action, "site report");
        }

        logger.debug("User {} authorized to {} site report {}", userEmail, action, reportId);
    }

    /**
     * Get list of project IDs the user has access to.
     * Results are cached to improve performance (cache expires after 5 minutes).
     *
     * @param userEmail User's email
     * @return List of accessible project IDs
     */
    @Cacheable(value = "userProjects", key = "#userEmail")
    @Transactional(readOnly = true)
    public List<Long> getAccessibleProjectIds(String userEmail) {
        logger.debug("Fetching accessible project IDs for user {} (cache miss)", userEmail);
        
        List<Long> projectIds = dashboardService.getProjectsForUser(userEmail)
            .stream()
            .map(Project::getId)
            .collect(Collectors.toList());

        logger.debug("User {} has access to {} projects", userEmail, projectIds.size());
        return projectIds;
    }

    /**
     * Check if user has access to a specific project.
     * Useful for validating project-level operations.
     *
     * @param userEmail User's email
     * @param projectId Project ID to check
     * @param action Action being performed (for logging)
     * @throws ResourceNotFoundException if project doesn't exist
     * @throws UnauthorizedException if user lacks access
     */
    @Transactional(readOnly = true)
    public void checkProjectAccess(String userEmail, Long projectId, String action) {
        logger.debug("Checking project access for user {} on project {}", userEmail, projectId);

        // Check if project exists
        boolean projectExists = projectRepository.existsById(projectId);
        if (!projectExists) {
            logger.warn("Project not found: {}", projectId);
            throw new ResourceNotFoundException("Project", projectId);
        }

        // Check if user has access
        List<Long> accessibleProjectIds = getAccessibleProjectIds(userEmail);
        if (!accessibleProjectIds.contains(projectId)) {
            logger.warn("User {} unauthorized to {} project {}", userEmail, action, projectId);
            throw new UnauthorizedException(action, "project");
        }

        logger.debug("User {} authorized to {} project {}", userEmail, action, projectId);
    }
}
