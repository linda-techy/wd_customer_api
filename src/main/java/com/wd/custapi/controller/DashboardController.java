package com.wd.custapi.controller;

import com.wd.custapi.dto.DashboardDto;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dashboard Controller - Main entry point for the app
 * Provides:
 * - Dashboard summary data
 * - Recent 5 projects
 * - Project search functionality
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    /**
     * Get full dashboard with summary, stats, and recent activities
     * GET /api/dashboard
     */
    @GetMapping
    public ResponseEntity<?> getDashboard(Authentication authentication) {
        try {
            String email = authentication.getName();
            DashboardDto dashboard = dashboardService.getCustomerDashboard(email);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            logger.error("Failed to load dashboard for user {}: {}", 
                authentication != null ? authentication.getName() : "unknown", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to load dashboard"));
        }
    }

    /**
     * Get recent N projects (default 5)
     * GET /api/dashboard/recent-projects?limit=5
     */
    @GetMapping("/recent-projects")
    public ResponseEntity<?> getRecentProjects(
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        try {
            if (limit < 1 || limit > 50) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Limit must be between 1 and 50"));
            }
            String email = authentication.getName();
            List<DashboardDto.ProjectCard> projects = dashboardService.getRecentProjects(email, limit);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            logger.error("Failed to fetch recent projects for user {}: {}", 
                authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch recent projects"));
        }
    }

    /**
     * Search projects by name, code, or location
     * GET /api/dashboard/search-projects?q=villa
     * If no search term provided, returns recent 5 projects
     */
    @GetMapping("/search-projects")
    public ResponseEntity<?> searchProjects(
            @RequestParam(required = false) String q,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            List<DashboardDto.ProjectCard> projects = dashboardService.searchProjects(email, q);
            return ResponseEntity.ok(projects);
        } catch (Exception e) {
            logger.error("Project search failed for user {}, query '{}': {}", 
                authentication.getName(), q, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Project search failed"));
        }
    }

    /**
     * Get detailed project information including progress and documents
     * GET /api/dashboard/projects/{projectUuid}
     * Returns: Project details, progress data, progress chart data, and documents
     */
    @GetMapping("/projects/{projectUuid}")
    public ResponseEntity<?> getProjectDetails(
            @PathVariable String projectUuid,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            DashboardDto.ProjectDetails details = dashboardService.getProjectDetails(projectUuid, email);
            if (details == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
            }
            return ResponseEntity.ok(details);
        } catch (SecurityException e) {
            logger.warn("Unauthorized access to project {} by user {}: {}", 
                projectUuid, authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You don't have access to this project"));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                logger.warn("Project not found: {} for user {}", projectUuid, authentication.getName());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
            }
            logger.error("Failed to fetch project details for {} by user {}: {}", 
                projectUuid, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch project details"));
        } catch (Exception e) {
            logger.error("Failed to fetch project details for {} by user {}: {}", 
                projectUuid, authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch project details"));
        }
    }

    /**
     * Update design package for a project
     * PUT /api/dashboard/projects/{projectUuid}/design-package
     * Request body: { "designPackage": "custom|premium|bespoke" }
     */
    @PutMapping("/projects/{projectUuid}/design-package")
    public ResponseEntity<?> updateDesignPackage(
            @PathVariable String projectUuid,
            @RequestBody java.util.Map<String, String> payload,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            String designPackage = payload.get("designPackage");

            if (designPackage == null || designPackage.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Design package cannot be empty"));
            }

            DashboardDto.ProjectDetails details = dashboardService.updateDesignPackage(projectUuid, designPackage, email);
            return ResponseEntity.ok(details);
        } catch (SecurityException e) {
            logger.warn("Unauthorized design package update for project {} by user {}: {}", 
                projectUuid, authentication.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You don't have permission to update this project"));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Project not found"));
            }
            logger.error("Failed to update design package for project {}: {}", 
                projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update design package"));
        } catch (Exception e) {
            logger.error("Failed to update design package for project {}: {}", 
                projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update design package"));
        }
    }
}
