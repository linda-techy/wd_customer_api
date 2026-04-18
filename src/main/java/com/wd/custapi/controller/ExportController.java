package com.wd.custapi.controller;

import com.wd.custapi.model.PaymentSchedule;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.PaymentScheduleRepository;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Export endpoints for customer-facing data.
 * All exports are restricted to the authenticated user's own projects.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/export")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'CUSTOMER_ADMIN')")
public class ExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    private final PaymentScheduleRepository paymentScheduleRepository;
    private final DashboardService dashboardService;

    public ExportController(
            PaymentScheduleRepository paymentScheduleRepository,
            DashboardService dashboardService) {
        this.paymentScheduleRepository = paymentScheduleRepository;
        this.dashboardService = dashboardService;
    }

    /**
     * Export payment schedules for a project as CSV.
     * Columns: Stage, Amount, Status, Due Date, Paid Date, Paid Amount
     */
    @GetMapping("/payments")
    public ResponseEntity<byte[]> exportPaymentsCsv(
            @PathVariable Long projectId,
            Authentication auth) {
        try {
            String email = auth.getName();

            // Role check — only financial roles may export payment data
            String role = dashboardService.getUserRole(email);
            if (!"CUSTOMER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
                return ResponseEntity.status(403).build();
            }

            // Verify the user has access to this specific project
            List<Project> userProjects = dashboardService.getProjectsForUser(email);
            boolean hasAccess = userProjects.stream()
                    .anyMatch(p -> p.getId().equals(projectId));

            if (!hasAccess) {
                logger.warn("Customer {} attempted to export payments for unauthorized project {}",
                        email, projectId);
                return ResponseEntity.status(403).build();
            }

            // Fetch payment schedules for the project (no pagination — export all)
            List<Long> projectIds = List.of(projectId);
            List<PaymentSchedule> schedules = paymentScheduleRepository
                    .findByProjectIdIn(projectIds,
                            org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                    .getContent();

            // Build CSV
            StringBuilder csv = new StringBuilder();
            csv.append('\uFEFF'); // UTF-8 BOM for Excel compatibility
            csv.append("Stage,Amount,Status,Due Date,Paid Date,Paid Amount\n");

            for (PaymentSchedule ps : schedules) {
                csv.append(escapeCsv(ps.getDescription())).append(',');
                csv.append(ps.getAmount() != null ? ps.getAmount().toPlainString() : "0").append(',');
                csv.append(escapeCsv(ps.getStatus())).append(',');
                csv.append(ps.getDueDate() != null ? ps.getDueDate().toString() : "").append(',');
                csv.append(ps.getPaidDate() != null ? ps.getPaidDate().toLocalDate().toString() : "").append(',');
                csv.append(ps.getPaidAmount() != null ? ps.getPaidAmount().toPlainString() : "0").append('\n');
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"project_payments_2026-04-18.csv\"")
                    .body(bytes);

        } catch (Exception e) {
            logger.error("Error exporting payments CSV for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /** Escape a CSV field — wrap in quotes if it contains comma, newline, or quote. */
    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
