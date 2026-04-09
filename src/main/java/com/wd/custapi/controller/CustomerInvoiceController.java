package com.wd.custapi.controller;

import com.wd.custapi.dto.CustomerInvoiceDto;
import com.wd.custapi.dto.ProjectModuleDtos.ApiResponse;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.ProjectInvoice;
import com.wd.custapi.repository.ProjectInvoiceRepository;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer-facing invoice API.
 * Customers can list and view invoices issued for their projects.
 * Only CUSTOMER and ADMIN roles can access financial invoice data.
 *
 * The portal API generates and manages all invoice records.
 * This controller is read-only.
 */
@RestController
@RequestMapping("/api/customer/invoices")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'CUSTOMER_ADMIN')")
public class CustomerInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerInvoiceController.class);

    private final ProjectInvoiceRepository invoiceRepository;
    private final DashboardService dashboardService;

    public CustomerInvoiceController(
            ProjectInvoiceRepository invoiceRepository,
            DashboardService dashboardService) {
        this.invoiceRepository = invoiceRepository;
        this.dashboardService = dashboardService;
    }

    /**
     * Returns true if the user's business role allows viewing financial invoice data.
     */
    private boolean canSeeFinancials(String email) {
        String role = dashboardService.getUserRole(email);
        return "CUSTOMER".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)
                || "CUSTOMER_ADMIN".equalsIgnoreCase(role);
    }

    /**
     * List invoices for a project (paginated, newest first).
     * DRAFT invoices are excluded — customers see only ISSUED, PAID, CANCELLED.
     *
     * GET /api/customer/invoices?projectId={uuid}&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerInvoiceDto>>> getInvoices(
            @RequestParam String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            String email = auth.getName();
            if (!canSeeFinancials(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Financial data is not available for your role", null));
            }

            // Verify the customer has access to this project
            Project project = dashboardService.getProjectByUuidAndEmail(projectId, email);
            Long projectDbId = project.getId();

            Pageable pageable = PageRequest.of(page, size);
            Page<ProjectInvoice> invoices = invoiceRepository.findByProjectIdExcludingDraft(projectDbId, pageable);
            Page<CustomerInvoiceDto> dtos = invoices.map(CustomerInvoiceDto::new);

            return ResponseEntity.ok(new ApiResponse<>(true, "Invoices retrieved successfully", dtos));

        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, msg, null));
            }
            if (msg != null && msg.toLowerCase().contains("access denied")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Access denied", null));
            }
            logger.error("Failed to fetch invoices for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve invoices", null));
        } catch (Exception e) {
            logger.error("Failed to fetch invoices for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve invoices", null));
        }
    }

    /**
     * Get a single invoice by ID.
     *
     * GET /api/customer/invoices/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerInvoiceDto>> getInvoiceById(
            @PathVariable Long id,
            Authentication auth) {
        try {
            String email = auth.getName();
            if (!canSeeFinancials(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Financial data is not available for your role", null));
            }

            ProjectInvoice invoice = invoiceRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            // Verify the customer has access to this project
            List<Project> userProjects = dashboardService.getProjectsForUser(email);
            boolean hasAccess = userProjects.stream()
                    .anyMatch(p -> p.getId().equals(invoice.getProject().getId()));
            if (!hasAccess) {
                logger.warn("Customer {} attempted to access invoice {} without project access", email, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse<>(false, "Access denied", null));
            }

            return ResponseEntity.ok(new ApiResponse<>(true, "Invoice retrieved successfully",
                    new CustomerInvoiceDto(invoice)));

        } catch (Exception e) {
            logger.error("Failed to fetch invoice id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Failed to retrieve invoice", null));
        }
    }
}
