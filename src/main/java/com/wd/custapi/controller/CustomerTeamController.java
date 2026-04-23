package com.wd.custapi.controller;

import com.wd.custapi.dto.TeamContactDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.service.CustomerTeamService;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/projects/{projectUuid}/team")
@PreAuthorize("hasAnyRole('CUSTOMER', 'CUSTOMER_ADMIN', 'ADMIN')")
public class CustomerTeamController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTeamController.class);

    private final CustomerTeamService teamService;
    private final DashboardService dashboardService;

    public CustomerTeamController(CustomerTeamService teamService, DashboardService dashboardService) {
        this.teamService = teamService;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<?> getTeam(
            @PathVariable String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<TeamContactDto> team = teamService.getTeamForProject(project.getId());
            return ResponseEntity.ok(team);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
            }
            logger.error("Failed to fetch team for project {}", projectUuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch team"));
        }
    }
}
