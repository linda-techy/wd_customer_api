package com.wd.custapi.controller;

import com.wd.custapi.dto.CustomerCctvCameraDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CctvCameraRepository;
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
@RequestMapping("/api/customer/projects/{projectId}/cctv-cameras")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'CUSTOMER_ADMIN')")
public class CustomerCctvController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerCctvController.class);

    private final CctvCameraRepository cameraRepository;
    private final DashboardService dashboardService;

    public CustomerCctvController(CctvCameraRepository cameraRepository,
                                   DashboardService dashboardService) {
        this.cameraRepository = cameraRepository;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCameras(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<CustomerCctvCameraDto> cameras = cameraRepository
                    .findByProjectIdAndIsActiveTrueOrderByDisplayOrder(project.getId())
                    .stream()
                    .map(CustomerCctvCameraDto::from)
                    .toList();
            return ResponseEntity.ok(Map.of("cameras", cameras, "count", cameras.size()));
        } catch (Exception e) {
            logger.error("Failed to fetch CCTV cameras for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch cameras"));
        }
    }
}
