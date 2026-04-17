package com.wd.custapi.controller;

import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Customer-facing financial lifecycle endpoints:
 *
 *   GET /api/projects/{projectId}/financial/stages           — stage payment summary with retention
 *   GET /api/projects/{projectId}/financial/variation-orders — approved VOs + payment schedules
 *   GET /api/projects/{projectId}/financial/deductions        — deduction register status
 *   GET /api/projects/{projectId}/financial/final-account     — final account view
 *   GET /api/projects/{projectId}/financial/summary           — combined financial snapshot
 */
@RestController
@RequestMapping("/api/projects/{projectId}/financial")
@PreAuthorize("isAuthenticated()")
public class CustomerFinancialController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerFinancialController.class);

    private final DashboardService dashboardService;
    private final PaymentStageRepository stageRepository;
    private final ChangeOrderRepository changeOrderRepository;
    private final ChangeOrderPaymentScheduleRepository scheduleRepository;
    private final DeductionRegisterRepository deductionRepository;
    private final FinalAccountRepository finalAccountRepository;

    public CustomerFinancialController(
            DashboardService dashboardService,
            PaymentStageRepository stageRepository,
            ChangeOrderRepository changeOrderRepository,
            ChangeOrderPaymentScheduleRepository scheduleRepository,
            DeductionRegisterRepository deductionRepository,
            FinalAccountRepository finalAccountRepository) {
        this.dashboardService       = dashboardService;
        this.stageRepository        = stageRepository;
        this.changeOrderRepository  = changeOrderRepository;
        this.scheduleRepository     = scheduleRepository;
        this.deductionRepository    = deductionRepository;
        this.finalAccountRepository = finalAccountRepository;
    }

    // ---- Stage payment summary (with certification / retention) ----

    @GetMapping("/stages")
    public ResponseEntity<Map<String, Object>> getStages(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<PaymentStage> stages = stageRepository.findByProjectIdOrderByStageNumberAsc(project.getId());

            List<Map<String, Object>> result = stages.stream().map(s -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("stageNumber", s.getStageNumber());
                m.put("stageName", s.getStageName());
                m.put("stageAmountInclGst", s.getStageAmountInclGst());
                m.put("netPayableAmount", s.getNetPayableAmount());
                m.put("paidAmount", s.getPaidAmount());
                m.put("status", s.getStatus());
                m.put("dueDate", s.getDueDate());
                m.put("certifiedBy", s.getCertifiedBy());
                m.put("certifiedAt", s.getCertifiedAt());
                m.put("retentionPct", s.getRetentionPct());
                m.put("retentionHeld", s.getRetentionHeld());
                m.put("milestoneDescription", s.getMilestoneDescription());
                return m;
            }).toList();

            return ResponseEntity.ok(Map.of("stages", result, "count", result.size()));
        } catch (Exception e) {
            logger.error("Failed to fetch stages for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch stages"));
        }
    }

    // ---- Approved variation orders with payment schedules ----

    @GetMapping("/variation-orders")
    public ResponseEntity<Map<String, Object>> getVariationOrders(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<ChangeOrder> orders = changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());

            List<Map<String, Object>> result = orders.stream().map(co -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", co.getId());
                m.put("referenceNumber", co.getReferenceNumber());
                m.put("title", co.getTitle());
                m.put("coType", co.getCoType());
                m.put("status", co.getStatus());
                m.put("voCategory", co.getVoCategory());
                m.put("netAmountInclGst", co.getNetAmountInclGst());
                m.put("approvedCost", co.getApprovedCost());
                m.put("advanceCollected", co.getAdvanceCollected());
                m.put("submittedAt", co.getSubmittedAt());
                m.put("approvedAt", co.getApprovedAt());
                // Attach payment schedule if present
                scheduleRepository.findByChangeOrderId(co.getId()).ifPresent(ps -> {
                    Map<String, Object> sched = new LinkedHashMap<>();
                    sched.put("advancePct",        ps.getAdvancePct());
                    sched.put("advanceAmount",      ps.getAdvanceAmount());
                    sched.put("advanceStatus",      ps.getAdvanceStatus());
                    sched.put("advanceDueDate",     ps.getAdvanceDueDate());
                    sched.put("progressPct",        ps.getProgressPct());
                    sched.put("progressAmount",     ps.getProgressAmount());
                    sched.put("progressStatus",     ps.getProgressStatus());
                    sched.put("completionPct",      ps.getCompletionPct());
                    sched.put("completionAmount",   ps.getCompletionAmount());
                    sched.put("completionStatus",   ps.getCompletionStatus());
                    m.put("paymentSchedule", sched);
                });
                return m;
            }).toList();

            return ResponseEntity.ok(Map.of("variationOrders", result, "count", result.size()));
        } catch (Exception e) {
            logger.error("Failed to fetch variation orders for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch variation orders"));
        }
    }

    // ---- Deduction register ----

    @GetMapping("/deductions")
    public ResponseEntity<Map<String, Object>> getDeductions(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<DeductionRegister> deductions =
                    deductionRepository.findByProjectIdOrderByCreatedAtDesc(project.getId());

            List<Map<String, Object>> result = deductions.stream().map(d -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", d.getId());
                m.put("itemDescription", d.getItemDescription());
                m.put("requestedAmount", d.getRequestedAmount());
                m.put("acceptedAmount", d.getAcceptedAmount());
                m.put("decision", d.getDecision());
                m.put("escalationStatus", d.getEscalationStatus());
                m.put("settledInFinalAccount", d.getSettledInFinalAccount());
                m.put("decisionDate", d.getDecisionDate());
                return m;
            }).toList();

            BigDecimal totalRequested = deductions.stream()
                    .map(d -> d.getRequestedAmount() != null ? d.getRequestedAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalAccepted = deductions.stream()
                    .filter(d -> d.getAcceptedAmount() != null)
                    .map(DeductionRegister::getAcceptedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return ResponseEntity.ok(Map.of(
                    "deductions", result,
                    "count", result.size(),
                    "totalRequestedAmount", totalRequested,
                    "totalAcceptedAmount", totalAccepted
            ));
        } catch (Exception e) {
            logger.error("Failed to fetch deductions for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch deductions"));
        }
    }

    // ---- Final account ----

    @GetMapping("/final-account")
    public ResponseEntity<Map<String, Object>> getFinalAccount(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            Optional<FinalAccount> opt = finalAccountRepository.findByProjectId(project.getId());
            if (opt.isEmpty()) {
                return ResponseEntity.ok(Map.of("finalAccount", null, "message", "Not yet prepared"));
            }
            FinalAccount fa = opt.get();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", fa.getId());
            m.put("status", fa.getStatus());
            m.put("baseContractValue", fa.getBaseContractValue());
            m.put("totalAdditions", fa.getTotalAdditions());
            m.put("totalAcceptedDeductions", fa.getTotalAcceptedDeductions());
            m.put("netRevisedContractValue", fa.getNetRevisedContractValue());
            m.put("totalReceivedToDate", fa.getTotalReceivedToDate());
            m.put("totalRetentionHeld", fa.getTotalRetentionHeld());
            m.put("balancePayable", fa.getBalancePayable());
            m.put("dlpStartDate", fa.getDlpStartDate());
            m.put("dlpEndDate", fa.getDlpEndDate());
            m.put("retentionReleased", fa.getRetentionReleased());
            m.put("retentionReleaseDate", fa.getRetentionReleaseDate());
            m.put("preparedBy", fa.getPreparedBy());
            m.put("agreedBy", fa.getAgreedBy());
            m.put("createdAt", fa.getCreatedAt());
            m.put("updatedAt", fa.getUpdatedAt());
            return ResponseEntity.ok(Map.of("finalAccount", m));
        } catch (Exception e) {
            logger.error("Failed to fetch final account for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch final account"));
        }
    }

    // ---- Combined financial snapshot ----

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            Long pid = project.getId();

            List<PaymentStage> stages = stageRepository.findByProjectIdOrderByStageNumberAsc(pid);
            long stagesPaid = stages.stream().filter(s -> com.wd.custapi.model.enums.PaymentStageStatus.PAID == s.getStatus()).count();
            BigDecimal totalRetentionHeld = stages.stream()
                    .filter(s -> s.getRetentionHeld() != null)
                    .map(PaymentStage::getRetentionHeld)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalPaid = stages.stream()
                    .filter(s -> s.getPaidAmount() != null)
                    .map(PaymentStage::getPaidAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long approvedVOs = changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(pid)
                    .stream().filter(co -> "APPROVED".equals(co.getStatus())).count();

            long pendingDeductions = deductionRepository.findByProjectIdOrderByCreatedAtDesc(pid)
                    .stream().filter(d -> "PENDING".equals(d.getDecision())).count();

            Optional<FinalAccount> fa = finalAccountRepository.findByProjectId(pid);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("totalStages", stages.size());
            summary.put("stagesPaid", stagesPaid);
            summary.put("totalPaidToDate", totalPaid);
            summary.put("totalRetentionHeld", totalRetentionHeld);
            summary.put("approvedVariationOrders", approvedVOs);
            summary.put("pendingDeductions", pendingDeductions);
            summary.put("finalAccountStatus", fa.map(FinalAccount::getStatus).orElse(null));
            fa.ifPresent(f -> {
                summary.put("balancePayable", f.getBalancePayable());
                summary.put("retentionReleased", f.getRetentionReleased());
            });

            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("Failed to build financial summary for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to build financial summary"));
        }
    }
}
