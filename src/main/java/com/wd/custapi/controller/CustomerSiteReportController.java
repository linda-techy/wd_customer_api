package com.wd.custapi.controller;

import com.wd.custapi.dto.CustomerSiteReportDto;
import com.wd.custapi.dto.ProjectModuleDtos.ApiResponse;
import com.wd.custapi.service.SiteReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Customer-facing site reports API.
 * Customers can only view site reports for their own projects.
 * All authorization is handled at the service layer.
 */
@RestController
@RequestMapping("/api/customer/site-reports")
// Tightened from the original sprawling list (was 9 roles including
// CONTRACTOR + BUILDER which aren't real roles in this seed). The
// customer-facing API is for the *project's customer*, plus admin
// access for support staff. Other roles use the Portal API.
@PreAuthorize("hasAnyRole('CUSTOMER', 'CUSTOMER_ADMIN', 'ADMIN')")
public class CustomerSiteReportController {

        private static final Logger logger = LoggerFactory.getLogger(CustomerSiteReportController.class);

        private final SiteReportService siteReportService;

        public CustomerSiteReportController(SiteReportService siteReportService) {
                this.siteReportService = siteReportService;
        }

        /**
         * Get all site reports for the current customer's projects.
         * Authorization is handled at service layer.
         */
        @GetMapping
        public ResponseEntity<ApiResponse<Page<CustomerSiteReportDto>>> getCustomerSiteReports(
                        @RequestParam(required = false) Long projectId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        Authentication auth) {

                String email = auth.getName();
                logger.info("Fetching site reports for user: {}, projectId: {}", email, projectId);

                Pageable pageable = PageRequest.of(page, size, Sort.by("reportDate").descending());
                Page<CustomerSiteReportDto> reports = siteReportService.getCustomerSiteReports(
                        email, projectId, pageable);

                return ResponseEntity.ok(new ApiResponse<>(true,
                        "Site reports retrieved successfully", reports));
        }

        /**
         * Per-project report-count summary for the current customer.
         * Drives the customer-app empty-state hint when a specific
         * project shows 0 reports but reports exist on other projects
         * (common scenario when the admin filed a report against the
         * wrong project from the portal dropdown).
         */
        @GetMapping("/summary")
        public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getSummary(
                        Authentication auth) {
                String email = auth.getName();
                java.util.List<java.util.Map<String, Object>> rows =
                                siteReportService.getReportSummaryForCustomer(email);
                return ResponseEntity.ok(new ApiResponse<>(true,
                                "Site report summary retrieved", rows));
        }

        /**
         * Get a specific site report by ID.
         * Authorization is handled at service layer.
         */
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<CustomerSiteReportDto>> getSiteReportById(
                        @PathVariable Long id,
                        Authentication auth) {

                String email = auth.getName();
                logger.info("Fetching site report {} for user {}", id, email);

                CustomerSiteReportDto report = siteReportService.getSiteReportById(email, id);

                return ResponseEntity.ok(new ApiResponse<>(true,
                        "Site report retrieved successfully", report));
        }
}
