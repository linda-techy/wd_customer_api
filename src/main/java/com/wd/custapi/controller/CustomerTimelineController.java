package com.wd.custapi.controller;

import com.wd.custapi.dto.TimelineResponseDto;
import com.wd.custapi.dto.TimelineSummaryDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.service.CustomerTimelineService;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer/projects/{projectUuid}/timeline")
@PreAuthorize("hasAnyRole('CUSTOMER', 'CUSTOMER_ADMIN', 'ADMIN')")
public class CustomerTimelineController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerTimelineController.class);

    private final CustomerTimelineService timelineService;
    private final DashboardService dashboardService;

    public CustomerTimelineController(CustomerTimelineService timelineService,
                                       DashboardService dashboardService) {
        this.timelineService = timelineService;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<?> getTimeline(
            @PathVariable String projectUuid,
            @RequestParam(defaultValue = "week") String bucket,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, auth.getName());
            TimelineResponseDto resp = timelineService.getTimeline(project.getId(), bucket, page, size);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        } catch (Exception e) {
            logger.error("Failed to fetch timeline for project {}", projectUuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch timeline"));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @PathVariable String projectUuid,
            Authentication auth) {
        try {
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, auth.getName());
            TimelineSummaryDto summary = timelineService.getSummary(project.getId());
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }
}
