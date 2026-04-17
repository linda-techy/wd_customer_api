package com.wd.custapi.controller;

import com.wd.custapi.dto.CustomerWarrantyDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.ProjectWarrantyRepository;
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
@RequestMapping("/api/customer/projects/{projectId}/warranties")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'CUSTOMER_ADMIN')")
public class CustomerWarrantyController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerWarrantyController.class);

    private final ProjectWarrantyRepository warrantyRepository;
    private final DashboardService dashboardService;

    public CustomerWarrantyController(ProjectWarrantyRepository warrantyRepository,
                                       DashboardService dashboardService) {
        this.warrantyRepository = warrantyRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWarranties(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<CustomerWarrantyDto> warranties = warrantyRepository
                    .findByProjectIdOrderByEndDateDesc(project.getId())
                    .stream()
                    .map(CustomerWarrantyDto::from)
                    .toList();
            return ResponseEntity.ok(Map.of("warranties", warranties, "count", warranties.size()));
        } catch (Exception e) {
            logger.error("Failed to fetch warranties for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch warranties"));
        }
    }
}
