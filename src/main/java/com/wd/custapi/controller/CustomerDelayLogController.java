package com.wd.custapi.controller;

import com.wd.custapi.dto.CustomerDelayLogDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.DelayLogRepository;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/projects/{projectId}/delays")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'CUSTOMER_ADMIN')")
public class CustomerDelayLogController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerDelayLogController.class);
    private final DelayLogRepository delayLogRepository;
    private final DashboardService dashboardService;

    public CustomerDelayLogController(DelayLogRepository delayLogRepository, DashboardService dashboardService) {
        this.delayLogRepository = delayLogRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDelayLogs(
            @PathVariable("projectId") String projectUuid, Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<CustomerDelayLogDto> delays = delayLogRepository
                    .findByProjectIdAndCustomerVisibleTrueOrderByFromDateDesc(project.getId())
                    .stream().map(CustomerDelayLogDto::from).toList();
            return ResponseEntity.ok(Map.of("delays", delays, "count", delays.size()));
        } catch (Exception e) {
            logger.error("Failed to fetch delay logs for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch delay logs"));
        }
    }
}
