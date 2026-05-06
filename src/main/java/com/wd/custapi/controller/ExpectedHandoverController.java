package com.wd.custapi.controller;

import com.wd.custapi.dto.ExpectedHandoverDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.service.DashboardService;
import com.wd.custapi.service.ExpectedHandoverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer-facing endpoint for the project's expected handover summary.
 *
 * <p>{@code GET /api/customer/projects/{uuid}/expected-handover} — gated on
 * the same role set as the delay-log endpoint. Project ownership is verified
 * via {@link DashboardService} BEFORE the service call so an unauthorised
 * UUID never reaches the cache. The authorised {@link Project} is then
 * passed to the service directly so the service doesn't need to re-resolve
 * the UUID via an admin-unscoped lookup.
 */
@RestController
@RequestMapping("/api/customer/projects/{projectId}/expected-handover")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'CUSTOMER_ADMIN')")
public class ExpectedHandoverController {

    private static final Logger logger = LoggerFactory.getLogger(ExpectedHandoverController.class);

    private final ExpectedHandoverService expectedHandoverService;
    private final DashboardService dashboardService;

    public ExpectedHandoverController(
            ExpectedHandoverService expectedHandoverService,
            DashboardService dashboardService) {
        this.expectedHandoverService = expectedHandoverService;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<ExpectedHandoverDto> get(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        String email = auth.getName();
        // Throws RuntimeException("Project not found or access denied") when
        // the caller is not authorised — handled by GlobalExceptionHandler.
        Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
        ExpectedHandoverDto dto = expectedHandoverService.compute(project);
        logger.debug("Returning expected-handover DTO for project {}", projectUuid);
        return ResponseEntity.ok(dto);
    }
}
