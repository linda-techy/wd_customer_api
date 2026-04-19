package com.wd.custapi.controller;

import com.wd.custapi.model.BoqDocument;
import com.wd.custapi.model.ChangeOrder;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.PaymentStage;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.BoqDocumentRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.PaymentStageRepository;
import com.wd.custapi.service.CustomerChangeOrderService;
import com.wd.custapi.service.DashboardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Customer-facing BOQ endpoints:
 *   - BOQ document status view
 *   - Payment schedule (read-only)
 *   - Change Order review (approve / reject)
 */
@RestController
@RequestMapping("/api/projects/{projectId}/boq")
@PreAuthorize("isAuthenticated()")
public class CustomerBoqController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerBoqController.class);

    private final DashboardService dashboardService;
    private final BoqDocumentRepository boqDocumentRepository;
    private final PaymentStageRepository paymentStageRepository;
    private final CustomerChangeOrderService changeOrderService;
    private final CustomerUserRepository customerUserRepository;

    public CustomerBoqController(DashboardService dashboardService,
                                   BoqDocumentRepository boqDocumentRepository,
                                   PaymentStageRepository paymentStageRepository,
                                   CustomerChangeOrderService changeOrderService,
                                   CustomerUserRepository customerUserRepository) {
        this.dashboardService = dashboardService;
        this.boqDocumentRepository = boqDocumentRepository;
        this.paymentStageRepository = paymentStageRepository;
        this.changeOrderService = changeOrderService;
        this.customerUserRepository = customerUserRepository;
    }

    // ---- BOQ Document ----

    @GetMapping("/document")
    public ResponseEntity<Map<String, Object>> getBoqDocument(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

            return boqDocumentRepository.findTopByProjectIdOrderByRevisionNumberDesc(project.getId())
                    .map(doc -> ResponseEntity.ok(boqDocumentToMap(doc)))
                    .orElse(ResponseEntity.status(404).body(
                            Map.of("success", false, "message", "No BOQ document found for this project")));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ document for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    /**
     * BOQ summary: approved (or latest non-rejected) document with embedded lean payment stages.
     * Includes pendingAcknowledgement flag so the app can prompt the customer to acknowledge.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getBoqSummary(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

            // Prefer approved document; fall back to latest non-rejected
            Optional<BoqDocument> docOpt = boqDocumentRepository.findByProjectIdAndStatus(
                    project.getId(), com.wd.custapi.model.enums.BoqDocumentStatus.APPROVED);
            if (docOpt.isEmpty()) {
                docOpt = boqDocumentRepository.findTopByProjectIdAndStatusNotOrderByRevisionNumberDesc(
                        project.getId(), com.wd.custapi.model.enums.BoqDocumentStatus.REJECTED);
            }
            if (docOpt.isEmpty()) {
                Map<String, Object> noBoq = new LinkedHashMap<>();
                noBoq.put("success", true);
                noBoq.put("message", "No BOQ available yet");
                noBoq.put("data", null);
                return ResponseEntity.ok(noBoq);
            }

            BoqDocument doc = docOpt.get();
            List<Map<String, Object>> stages = paymentStageRepository
                    .findByBoqDocumentIdOrderByStageNumberAsc(doc.getId())
                    .stream().map(this::stageSummaryToMap).toList();

            return ResponseEntity.ok(Map.of("success", true, "data", boqSummaryToMap(doc, stages)));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ summary for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    /**
     * Payment stages for the project — lean customer view, no internal billing fields.
     */
    @GetMapping("/payment-stages")
    public ResponseEntity<Map<String, Object>> getBoqPaymentStages(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

            List<Map<String, Object>> stages = paymentStageRepository
                    .findByProjectIdOrderByStageNumberAsc(project.getId())
                    .stream().map(this::stageSummaryToMap).toList();

            return ResponseEntity.ok(Map.of("success", true, "stages", stages));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ payment stages for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    /**
     * Records the customer's digital acknowledgement of a BOQ document. Idempotent.
     * Cross-checks document belongs to the requested project before writing.
     */
    @Transactional
    @PatchMapping("/documents/{documentId}/acknowledge")
    public ResponseEntity<Map<String, Object>> acknowledgeBoq(
            @PathVariable("projectId") String projectUuid,
            @PathVariable Long documentId,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

            BoqDocument doc = boqDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalArgumentException("BOQ document not found: " + documentId));

            if (!doc.getProject().getId().equals(project.getId())) {
                return ResponseEntity.status(403).body(
                        Map.of("success", false, "message", "Document does not belong to this project"));
            }

            // Idempotency: if already acknowledged, skip write and return current state
            if (doc.getCustomerAcknowledgedAt() != null) {
                List<Map<String, Object>> existingStages = paymentStageRepository
                        .findByBoqDocumentIdOrderByStageNumberAsc(documentId)
                        .stream().map(this::stageSummaryToMap).toList();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "BOQ acknowledged",
                        "data", boqSummaryToMap(doc, existingStages)));
            }

            CustomerUser customer = customerUserRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));

            boqDocumentRepository.recordAcknowledgement(documentId, LocalDateTime.now(), customer.getId());

            // Reload after update to get fresh timestamps (JPQL update does not update in-memory entity)
            BoqDocument updated = boqDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new IllegalStateException("Document disappeared after acknowledge"));

            List<Map<String, Object>> stages = paymentStageRepository
                    .findByBoqDocumentIdOrderByStageNumberAsc(documentId)
                    .stream().map(this::stageSummaryToMap).toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "BOQ acknowledged",
                    "data", boqSummaryToMap(updated, stages)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to acknowledge BOQ document {} for project {}", documentId, projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    // ---- Payment Schedule ----

    @GetMapping("/payment-schedule")
    public ResponseEntity<Map<String, Object>> getPaymentSchedule(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

            List<Map<String, Object>> stages = paymentStageRepository
                    .findByProjectIdOrderByStageNumberAsc(project.getId())
                    .stream()
                    .map(this::stageToMap)
                    .toList();

            // Summary totals
            BigDecimal totalContractValue = stages.stream()
                    .map(s -> (BigDecimal) s.get("stageAmountInclGst"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalPaid = stages.stream()
                    .map(s -> (BigDecimal) s.get("paidAmount"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalOutstanding = stages.stream()
                    .filter(s -> !"PAID".equals(s.get("status")))
                    .map(s -> (BigDecimal) s.get("netPayableAmount"))
                    .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stages", stages,
                    "summary", Map.of(
                            "totalContractValue", totalContractValue,
                            "totalPaid", totalPaid,
                            "totalOutstanding", totalOutstanding,
                            "stageCount", stages.size()
                    )
            ));
        } catch (Exception e) {
            logger.error("Failed to fetch payment schedule for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    // ---- Change Orders ----

    @GetMapping("/change-orders")
    public ResponseEntity<Map<String, Object>> getChangeOrders(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

            List<Map<String, Object>> cos = changeOrderService
                    .getProjectChangeOrders(project.getId())
                    .stream().map(this::changeOrderToMap).toList();

            return ResponseEntity.ok(Map.of("success", true, "changeOrders", cos));
        } catch (Exception e) {
            logger.error("Failed to fetch change orders for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    @GetMapping("/change-orders/pending-review")
    public ResponseEntity<Map<String, Object>> getPendingReview(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);

            List<Map<String, Object>> cos = changeOrderService
                    .getPendingReview(project.getId())
                    .stream().map(this::changeOrderToMap).toList();

            return ResponseEntity.ok(Map.of("success", true, "changeOrders", cos));
        } catch (Exception e) {
            logger.error("Failed to fetch pending COs for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    @GetMapping("/change-orders/{coId}")
    public ResponseEntity<Map<String, Object>> getChangeOrder(
            @PathVariable("projectId") String projectUuid,
            @PathVariable Long coId,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            ChangeOrder co = changeOrderService.getChangeOrder(coId, project.getId());
            return ResponseEntity.ok(Map.of("success", true, "changeOrder", changeOrderToMap(co)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to fetch change order {} for project {}", coId, projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    @PatchMapping("/change-orders/{coId}/approve")
    public ResponseEntity<Map<String, Object>> approveChangeOrder(
            @PathVariable("projectId") String projectUuid,
            @PathVariable Long coId,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            ChangeOrder co = changeOrderService.approve(coId, project.getId(), email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Change order approved successfully",
                    "changeOrder", changeOrderToMap(co)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to approve change order {} for project {}", coId, projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    @PatchMapping("/change-orders/{coId}/reject")
    public ResponseEntity<Map<String, Object>> rejectChangeOrder(
            @PathVariable("projectId") String projectUuid,
            @PathVariable Long coId,
            @Valid @RequestBody RejectCoRequest request,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            ChangeOrder co = changeOrderService.reject(coId, project.getId(), email, request.reason());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Change order rejected",
                    "changeOrder", changeOrderToMap(co)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to reject change order {} for project {}", coId, projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    // ---- Mappers ----

    private Map<String, Object> boqDocumentToMap(BoqDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("status", d.getStatus());
        m.put("totalValueExGst", d.getTotalValueExGst());
        m.put("gstRate", d.getGstRate());
        m.put("totalGstAmount", d.getTotalGstAmount());
        m.put("totalValueInclGst", d.getTotalValueInclGst());
        m.put("revisionNumber", d.getRevisionNumber());
        m.put("submittedAt", d.getSubmittedAt());
        m.put("approvedAt", d.getApprovedAt());
        m.put("customerApprovedAt", d.getCustomerApprovedAt());
        m.put("rejectedAt", d.getRejectedAt());
        m.put("rejectionReason", d.getRejectionReason());
        return m;
    }

    private Map<String, Object> stageToMap(PaymentStage s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("stageNumber", s.getStageNumber());
        m.put("stageName", s.getStageName());
        m.put("stagePercentage", s.getStagePercentage());
        m.put("stageAmountExGst", s.getStageAmountExGst());
        m.put("gstAmount", s.getGstAmount());
        m.put("stageAmountInclGst", s.getStageAmountInclGst());
        m.put("appliedCreditAmount", s.getAppliedCreditAmount());
        m.put("netPayableAmount", s.getNetPayableAmount());
        m.put("paidAmount", s.getPaidAmount() != null ? s.getPaidAmount() : BigDecimal.ZERO);
        m.put("status", s.getStatus());
        m.put("dueDate", s.getDueDate());
        m.put("milestoneDescription", s.getMilestoneDescription());
        m.put("paidAt", s.getPaidAt());
        return m;
    }

    private Map<String, Object> changeOrderToMap(ChangeOrder co) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", co.getId());
        m.put("referenceNumber", co.getReferenceNumber());
        m.put("coType", co.getCoType());
        m.put("status", co.getStatus());
        m.put("title", co.getTitle());
        m.put("description", co.getDescription());
        m.put("justification", co.getJustification());
        m.put("netAmountExGst", co.getNetAmountExGst());
        m.put("gstAmount", co.getGstAmount());
        m.put("netAmountInclGst", co.getNetAmountInclGst());
        m.put("submittedAt", co.getSubmittedAt());
        m.put("approvedAt", co.getApprovedAt());
        m.put("rejectedAt", co.getRejectedAt());
        m.put("rejectionReason", co.getRejectionReason());
        m.put("createdAt", co.getCreatedAt());
        return m;
    }

    /**
     * Lean payment stage view for customer — excludes internal billing fields.
     */
    private Map<String, Object> stageSummaryToMap(PaymentStage s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stageNumber", s.getStageNumber());
        m.put("stageName", s.getStageName());
        m.put("stageAmountExGst", s.getStageAmountExGst());
        m.put("gstAmount", s.getGstAmount());
        m.put("stageAmountInclGst", s.getStageAmountInclGst());
        m.put("stagePercentage", s.getStagePercentage());
        m.put("status", s.getStatus());
        m.put("dueDate", s.getDueDate());
        m.put("milestoneDescription", s.getMilestoneDescription());
        return m;
    }

    private Map<String, Object> boqSummaryToMap(BoqDocument doc, List<Map<String, Object>> stages) {
        boolean pendingAcknowledgement = com.wd.custapi.model.enums.BoqDocumentStatus.APPROVED == doc.getStatus()
                && doc.getCustomerAcknowledgedAt() == null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("documentId", doc.getId());
        m.put("projectId", doc.getProject().getId()); // project is lazy but session is open here
        m.put("totalValueExGst", doc.getTotalValueExGst());
        m.put("totalGstAmount", doc.getTotalGstAmount());
        m.put("totalValueInclGst", doc.getTotalValueInclGst());
        m.put("gstRate", doc.getGstRate());
        m.put("status", doc.getStatus());
        m.put("revisionNumber", doc.getRevisionNumber());
        m.put("approvedAt", doc.getCustomerApprovedAt());
        m.put("acknowledgedAt", doc.getCustomerAcknowledgedAt());
        m.put("pendingAcknowledgement", pendingAcknowledgement);
        m.put("paymentStages", stages);
        return m;
    }

    // ---- Request DTOs ----

    public record RejectCoRequest(
            @NotBlank @Size(max = 1000) String reason
    ) {}
}
