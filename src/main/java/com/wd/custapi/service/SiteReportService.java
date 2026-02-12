package com.wd.custapi.service;

import com.wd.custapi.dto.CustomerSiteReportDto;
import com.wd.custapi.exception.ResourceNotFoundException;
import com.wd.custapi.exception.UnauthorizedException;
import com.wd.custapi.model.SiteReport;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.SiteReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for site report operations.
 * Handles business logic and authorization for customer site reports.
 */
@Service
public class SiteReportService {

    private static final Logger logger = LoggerFactory.getLogger(SiteReportService.class);

    private final SiteReportRepository siteReportRepository;
    private final ProjectRepository projectRepository;
    private final AuthorizationService authorizationService;

    public SiteReportService(SiteReportRepository siteReportRepository,
                            ProjectRepository projectRepository,
                            AuthorizationService authorizationService) {
        this.siteReportRepository = siteReportRepository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
    }

    /**
     * Get paginated site reports for the authenticated customer.
     * Optionally filter by project ID.
     *
     * @param userEmail User's email
     * @param projectId Optional project ID to filter by
     * @param pageable Pagination parameters
     * @return Page of CustomerSiteReportDto
     * @throws UnauthorizedException if user lacks access to specified project
     * @throws ResourceNotFoundException if specified project doesn't exist
     */
    @Transactional(readOnly = true, timeout = 5)
    public Page<CustomerSiteReportDto> getCustomerSiteReports(
            String userEmail,
            Long projectId,
            Pageable pageable) {

        logger.debug("Fetching site reports for user: {}, projectId: {}", userEmail, projectId);

        List<Long> projectIds = authorizationService.getAccessibleProjectIds(userEmail);

        if (projectIds.isEmpty()) {
            logger.info("No projects found for customer: {}", userEmail);
            return Page.empty();
        }

        // Filter by specific project if provided
        if (projectId != null) {
            // Check if user has access to the specified project
            if (!projectIds.contains(projectId)) {
                // Check if project exists to return correct error
                if (projectRepository.existsById(projectId)) {
                    logger.warn("User {} unauthorized to view project {}", userEmail, projectId);
                    throw new UnauthorizedException("view", "project");
                } else {
                    logger.warn("Project not found: {}", projectId);
                    throw new ResourceNotFoundException("Project", projectId);
                }
            }
            projectIds = List.of(projectId);
        }

        Page<SiteReport> reports = siteReportRepository.findByProjectIdIn(projectIds, pageable);
        Page<CustomerSiteReportDto> reportDtos = reports.map(CustomerSiteReportDto::new);

        logger.debug("Found {} site reports for user {}", reports.getTotalElements(), userEmail);
        return reportDtos;
    }

    /**
     * Get a specific site report by ID.
     * Validates that the user has access to the report's project.
     *
     * @param userEmail User's email
     * @param reportId Report ID
     * @return CustomerSiteReportDto
     * @throws ResourceNotFoundException if report doesn't exist
     * @throws UnauthorizedException if user lacks access
     */
    @Transactional(readOnly = true)
    public CustomerSiteReportDto getSiteReportById(String userEmail, Long reportId) {
        logger.debug("Fetching site report {} for user {}", reportId, userEmail);

        // Authorization check (throws exception if unauthorized)
        authorizationService.checkSiteReportAccess(userEmail, reportId, "view");

        // Fetch report (guaranteed to exist if authorization passed)
        SiteReport report = siteReportRepository.findById(reportId)
            .orElseThrow(() -> {
                logger.error("Site report {} not found after authorization check", reportId);
                return new ResourceNotFoundException("SiteReport", reportId);
            });

        logger.debug("Successfully retrieved site report {} for user {}", reportId, userEmail);
        return new CustomerSiteReportDto(report);
    }
}
