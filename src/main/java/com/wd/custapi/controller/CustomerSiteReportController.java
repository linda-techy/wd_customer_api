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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Customer-facing site reports API.
 * Customers can only view site reports for their own projects.
 * All authorization is handled at the service layer.
 */
@RestController
@RequestMapping("/api/customer/site-reports")
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
