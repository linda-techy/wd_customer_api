package com.wd.custapi.controller;

import com.wd.custapi.dto.DashboardDto;
import com.wd.custapi.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @Autowired
    private DashboardService dashboardService;

    /**
     * Get full dashboard with summary, stats, and recent activities
     * GET /api/dashboard
     */
    @GetMapping
    public ResponseEntity<DashboardDto> getDashboard(Authentication authentication) {
        String email = authentication.getName();
        DashboardDto dashboard = dashboardService.getCustomerDashboard(email);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get recent N projects (default 5)
     * GET /api/dashboard/recent-projects?limit=5
     */
    @GetMapping("/recent-projects")
    public ResponseEntity<List<DashboardDto.ProjectCard>> getRecentProjects(
            @RequestParam(defaultValue = "5") int limit,
            Authentication authentication) {
        String email = authentication.getName();
        List<DashboardDto.ProjectCard> projects = dashboardService.getRecentProjects(email, limit);
        return ResponseEntity.ok(projects);
    }

    /**
     * Search projects by name, code, or location
     * GET /api/dashboard/search-projects?q=villa
     * If no search term provided, returns recent 5 projects
     */
    @GetMapping("/search-projects")
    public ResponseEntity<List<DashboardDto.ProjectCard>> searchProjects(
            @RequestParam(required = false) String q,
            Authentication authentication) {
        String email = authentication.getName();
        List<DashboardDto.ProjectCard> projects = dashboardService.searchProjects(email, q);
        return ResponseEntity.ok(projects);
    }

    /**
     * Get detailed project information including progress and documents
     * GET /api/dashboard/projects/{projectId}
     * Returns: Project details, progress data, progress chart data, and documents
     */
    @GetMapping("/projects/{projectUuid}")
    public ResponseEntity<DashboardDto.ProjectDetails> getProjectDetails(
            @PathVariable String projectUuid,
            Authentication authentication) {
        String email = authentication.getName();
        DashboardDto.ProjectDetails details = dashboardService.getProjectDetails(projectUuid, email);
        return ResponseEntity.ok(details);
    }

    /**
     * Update design package for a project
     * PUT /api/dashboard/projects/{projectUuid}/design-package
     * Request body: { "designPackage": "custom|premium|bespoke" }
     */
    @PutMapping("/projects/{projectUuid}/design-package")
    public ResponseEntity<DashboardDto.ProjectDetails> updateDesignPackage(
            @PathVariable String projectUuid,
            @RequestBody java.util.Map<String, String> payload,
            Authentication authentication) {
        String email = authentication.getName();
        String designPackage = payload.get("designPackage");

        if (designPackage == null) {
            return ResponseEntity.badRequest().build();
        }

        DashboardDto.ProjectDetails details = dashboardService.updateDesignPackage(projectUuid, designPackage, email);
        return ResponseEntity.ok(details);
    }
}
