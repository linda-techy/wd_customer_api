package com.wd.custapi.controller;

import com.wd.custapi.dto.CustomerSiteReportDto;
import com.wd.custapi.dto.ProjectModuleDtos.ApiResponse;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.SiteReport;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.SiteReportRepository;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer-facing site reports API.
 * Customers can only view site reports for their own projects.
 */
@RestController
@RequestMapping("/api/customer/site-reports")
public class CustomerSiteReportController {

        private static final Logger logger = LoggerFactory.getLogger(CustomerSiteReportController.class);

        private final SiteReportRepository siteReportRepository;
        private final ProjectRepository projectRepository;
        private final DashboardService dashboardService;

        public CustomerSiteReportController(
                        SiteReportRepository siteReportRepository,
                        ProjectRepository projectRepository,
                        DashboardService dashboardService) {
                this.siteReportRepository = siteReportRepository;
                this.projectRepository = projectRepository;
                this.dashboardService = dashboardService;
        }

        /**
         * Get all site reports for the current customer's projects.
         */
        @GetMapping
        public ResponseEntity<ApiResponse<Page<CustomerSiteReportDto>>> getCustomerSiteReports(
                        @RequestParam(required = false) Long projectId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Authentication auth) {
                try {
                        String email = auth.getName();
                        logger.info("Fetching site reports for user: {}, projectId: {}", email, projectId);

                        List<Project> userProjects = dashboardService.getProjectsForUser(email);
                        List<Long> projectIds = userProjects.stream()
                                        .map(Project::getId)
                                        .collect(Collectors.toList());

                        if (projectIds.isEmpty()) {
                                logger.info("No projects found for customer: {}", email);
                                return ResponseEntity.ok(new ApiResponse<>(true,
                                                "No projects found for customer", Page.empty()));
                        }

                        // Filter by specific project if provided
                        if (projectId != null) {
                                if (!projectIds.contains(projectId)) {
                                        boolean existsInDb = projectRepository.existsById(projectId);
                                        logger.warn("Unauthorized access attempt by {} to project {} (Exists in DB: {})",
                                                        email, projectId, existsInDb);

                                        String errorMsg = existsInDb ? "Access denied to this project"
                                                        : "Project not found";
                                        return ResponseEntity.status(existsInDb ? 403 : 404)
                                                        .body(new ApiResponse<>(false, errorMsg, null));
                                }
                                projectIds = List.of(projectId);
                        }

                        Pageable pageable = PageRequest.of(page, size, Sort.by("reportDate").descending());
                        Page<SiteReport> reports = siteReportRepository.findByProjectIdIn(projectIds, pageable);
                        Page<CustomerSiteReportDto> reportDtos = reports.map(CustomerSiteReportDto::new);

                        return ResponseEntity.ok(new ApiResponse<>(true,
                                        "Site reports retrieved successfully", reportDtos));

                } catch (Exception e) {
                        logger.error("Error fetching customer site reports: {}", e.getMessage(), e);
                        return ResponseEntity.status(500)
                                        .body(new ApiResponse<>(false,
                                                        "Failed to retrieve site reports: " + e.getMessage(), null));
                }
        }

        /**
         * Get a specific site report by ID.
         * Verifies the customer has access to the project.
         */
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<CustomerSiteReportDto>> getSiteReportById(
                        @PathVariable Long id,
                        Authentication auth) {
                try {
                        String email = auth.getName();

                        SiteReport report = siteReportRepository.findById(id).orElse(null);
                        if (report == null) {
                                return ResponseEntity.status(404)
                                                .body(new ApiResponse<>(false, "Site report not found with id: " + id,
                                                                null));
                        }

                        // Verify customer has access to this project
                        List<Project> userProjects = dashboardService.getProjectsForUser(email);
                        boolean hasAccess = report.getProject() != null && userProjects.stream()
                                        .anyMatch(p -> p.getId().equals(report.getProject().getId()));

                        if (!hasAccess) {
                                logger.warn("Customer {} attempted to access unauthorized report {}", email, id);
                                return ResponseEntity.status(403)
                                                .body(new ApiResponse<>(false, "Access denied to this report", null));
                        }

                        CustomerSiteReportDto reportDto = new CustomerSiteReportDto(report);
                        return ResponseEntity.ok(new ApiResponse<>(true,
                                        "Site report retrieved successfully", reportDto));

                } catch (Exception e) {
                        logger.error("Error fetching site report id={}: {}", id, e.getMessage(), e);
                        return ResponseEntity.status(500)
                                        .body(new ApiResponse<>(false,
                                                        "Failed to retrieve site report: " + e.getMessage(), null));
                }
        }
}
