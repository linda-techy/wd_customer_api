package com.wd.custapi.controller;

import com.wd.custapi.dto.CustomerPaymentScheduleDto;
import com.wd.custapi.dto.ProjectModuleDtos.ApiResponse;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.PaymentSchedule;
import com.wd.custapi.repository.PaymentScheduleRepository;
import com.wd.custapi.repository.ProjectRepository;
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
 * Customer-facing payment API.
 * Customers can view payment schedules and transactions for their own projects only.
 */
@RestController
@RequestMapping("/api/customer/payments")
public class CustomerPaymentController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerPaymentController.class);

    private final PaymentScheduleRepository paymentScheduleRepository;
    private final ProjectRepository projectRepository;
    private final DashboardService dashboardService;

    public CustomerPaymentController(
            PaymentScheduleRepository paymentScheduleRepository,
            ProjectRepository projectRepository,
            DashboardService dashboardService) {
        this.paymentScheduleRepository = paymentScheduleRepository;
        this.projectRepository = projectRepository;
        this.dashboardService = dashboardService;
    }

    /**
     * Get all payment schedules for the current customer's projects.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerPaymentScheduleDto>>> getCustomerPayments(
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            String email = auth.getName();
            List<Project> userProjects = dashboardService.getProjectsForUser(email);
            List<Long> projectIds = userProjects.stream()
                    .map(Project::getId)
                    .collect(Collectors.toList());

            if (projectIds.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(true,
                        "No projects found for customer", Page.empty()));
            }

            // Filter by specific project if provided
            if (projectId != null) {
                if (!projectIds.contains(projectId)) {
                    logger.warn("Customer {} attempted to access payments for unauthorized project {}",
                            email, projectId);
                    return ResponseEntity.status(403)
                            .body(new ApiResponse<>(false, "Access denied to this project", null));
                }
                projectIds = List.of(projectId);
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").ascending());
            Page<PaymentSchedule> payments = paymentScheduleRepository.findByProjectIdIn(projectIds, pageable);
            Page<CustomerPaymentScheduleDto> paymentDtos = payments.map(CustomerPaymentScheduleDto::new);

            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Payment schedules retrieved successfully", paymentDtos));

        } catch (Exception e) {
            logger.error("Error fetching customer payments: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Failed to retrieve payment schedules: " + e.getMessage(), null));
        }
    }

    /**
     * Get a specific payment schedule by ID.
     * Verifies the customer has access to the project.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerPaymentScheduleDto>> getPaymentScheduleById(
            @PathVariable Long id,
            Authentication auth) {
        try {
            String email = auth.getName();

            PaymentSchedule schedule = paymentScheduleRepository.findById(id).orElse(null);
            if (schedule == null) {
                return ResponseEntity.status(404)
                        .body(new ApiResponse<>(false, "Payment schedule not found", null));
            }

            // Resolve project via DesignPackagePayment -> Project
            Long scheduleProjectId = schedule.getDesignPayment() != null
                    && schedule.getDesignPayment().getProject() != null
                            ? schedule.getDesignPayment().getProject().getId()
                            : null;

            if (scheduleProjectId == null) {
                logger.error("Payment schedule {} has no associated project", id);
                return ResponseEntity.status(500)
                        .body(new ApiResponse<>(false, "Invalid payment schedule data", null));
            }

            // Verify customer has access to this project
            List<Project> userProjects = dashboardService.getProjectsForUser(email);
            boolean hasAccess = userProjects.stream()
                    .anyMatch(p -> p.getId().equals(scheduleProjectId));

            if (!hasAccess) {
                logger.warn("Customer {} attempted to access unauthorized payment schedule {}",
                        email, id);
                return ResponseEntity.status(403)
                        .body(new ApiResponse<>(false, "Access denied to this payment schedule", null));
            }

            CustomerPaymentScheduleDto scheduleDto = new CustomerPaymentScheduleDto(schedule);
            return ResponseEntity.ok(new ApiResponse<>(true,
                    "Payment schedule retrieved successfully", scheduleDto));

        } catch (Exception e) {
            logger.error("Error fetching payment schedule id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new ApiResponse<>(false, "Failed to retrieve payment schedule: " + e.getMessage(), null));
        }
    }
}
