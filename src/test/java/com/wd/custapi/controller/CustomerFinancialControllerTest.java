package com.wd.custapi.controller;

import com.wd.custapi.model.ChangeOrder;
import com.wd.custapi.model.ChangeOrderPaymentSchedule;
import com.wd.custapi.model.DeductionRegister;
import com.wd.custapi.model.FinalAccount;
import com.wd.custapi.model.PaymentStage;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.enums.InvoiceStatus;
import com.wd.custapi.model.enums.PaymentStageStatus;
import com.wd.custapi.repository.BoqInvoiceRepository;
import com.wd.custapi.repository.ChangeOrderPaymentScheduleRepository;
import com.wd.custapi.repository.ChangeOrderRepository;
import com.wd.custapi.repository.DeductionRegisterRepository;
import com.wd.custapi.repository.FinalAccountRepository;
import com.wd.custapi.repository.PaymentStageRepository;
import com.wd.custapi.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Pure-Mockito unit tests for {@link CustomerFinancialController}. Every
 * constructor dependency is mocked; entities are deep-mocked only for the
 * getters the controller actually reads. Each endpoint is exercised for its
 * happy path AND the catch-all 500 branch (service/repo throws).
 *
 * <p>Project-ownership enforcement (via {@link DashboardService}) is asserted —
 * the controller must resolve the project from {@code (uuid, email)} before any
 * repository read so a customer can't read another project's financials.
 */
@ExtendWith(MockitoExtension.class)
class CustomerFinancialControllerTest {

    @Mock private DashboardService dashboardService;
    @Mock private PaymentStageRepository stageRepository;
    @Mock private ChangeOrderRepository changeOrderRepository;
    @Mock private ChangeOrderPaymentScheduleRepository scheduleRepository;
    @Mock private DeductionRegisterRepository deductionRepository;
    @Mock private FinalAccountRepository finalAccountRepository;
    @Mock private BoqInvoiceRepository boqInvoiceRepository;
    @Mock private Authentication auth;

    @InjectMocks
    private CustomerFinancialController controller;

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(7L);
        lenient().when(auth.getName()).thenReturn("customer@example.com");
        lenient().when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenReturn(project);
    }

    // ---- /stages ----

    @Test
    void getStages_success_returnsMappedListWithCount() {
        PaymentStage s = mock(PaymentStage.class);
        when(s.getId()).thenReturn(1L);
        when(s.getStageNumber()).thenReturn(1);
        when(stageRepository.findByProjectIdOrderByStageNumberAsc(7L)).thenReturn(List.of(s));

        ResponseEntity<Map<String, Object>> r = controller.getStages("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("count", 1);
        assertThat(r.getBody()).containsKey("stages");
        verify(dashboardService).getProjectByUuidAndEmail("uuid", "customer@example.com");
    }

    @Test
    void getStages_emptyList_returnsZeroCount() {
        when(stageRepository.findByProjectIdOrderByStageNumberAsc(7L)).thenReturn(List.of());

        ResponseEntity<Map<String, Object>> r = controller.getStages("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("count", 0);
    }

    @Test
    void getStages_serviceThrows_returns500() {
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenThrow(new RuntimeException("not a member"));

        ResponseEntity<Map<String, Object>> r = controller.getStages("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody()).containsEntry("error", "Failed to fetch stages");
    }

    // ---- /variation-orders ----

    @Test
    void getVariationOrders_withSchedule_attachesPaymentSchedule() {
        ChangeOrder co = mock(ChangeOrder.class);
        when(co.getId()).thenReturn(11L);
        ChangeOrderPaymentSchedule sched = mock(ChangeOrderPaymentSchedule.class);
        when(changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(co));
        when(scheduleRepository.findByChangeOrderId(11L)).thenReturn(Optional.of(sched));

        ResponseEntity<Map<String, Object>> r = controller.getVariationOrders("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("count", 1);
        verify(scheduleRepository).findByChangeOrderId(11L);
    }

    @Test
    void getVariationOrders_noSchedule_stillReturnsOrder() {
        ChangeOrder co = mock(ChangeOrder.class);
        when(co.getId()).thenReturn(11L);
        when(changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(co));
        when(scheduleRepository.findByChangeOrderId(11L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> r = controller.getVariationOrders("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("count", 1);
    }

    @Test
    void getVariationOrders_repoThrows_returns500() {
        when(changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(7L))
                .thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> r = controller.getVariationOrders("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody()).containsEntry("error", "Failed to fetch variation orders");
    }

    // ---- /deductions ----

    @Test
    void getDeductions_computesRequestedAndAcceptedTotals() {
        DeductionRegister d1 = mock(DeductionRegister.class);
        when(d1.getRequestedAmount()).thenReturn(new BigDecimal("100.00"));
        when(d1.getAcceptedAmount()).thenReturn(new BigDecimal("80.00"));
        DeductionRegister d2 = mock(DeductionRegister.class);
        when(d2.getRequestedAmount()).thenReturn(new BigDecimal("50.00"));
        when(d2.getAcceptedAmount()).thenReturn(null); // excluded from accepted total
        when(deductionRepository.findByProjectIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(d1, d2));

        ResponseEntity<Map<String, Object>> r = controller.getDeductions("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("count", 2);
        assertThat(r.getBody()).containsEntry("totalRequestedAmount", new BigDecimal("150.00"));
        assertThat(r.getBody()).containsEntry("totalAcceptedAmount", new BigDecimal("80.00"));
    }

    @Test
    void getDeductions_nullRequestedAmount_treatedAsZero() {
        DeductionRegister d = mock(DeductionRegister.class);
        when(d.getRequestedAmount()).thenReturn(null);
        when(d.getAcceptedAmount()).thenReturn(null);
        when(deductionRepository.findByProjectIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(d));

        ResponseEntity<Map<String, Object>> r = controller.getDeductions("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("totalRequestedAmount", BigDecimal.ZERO);
        assertThat(r.getBody()).containsEntry("totalAcceptedAmount", BigDecimal.ZERO);
    }

    @Test
    void getDeductions_repoThrows_returns500() {
        when(deductionRepository.findByProjectIdOrderByCreatedAtDesc(7L))
                .thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> r = controller.getDeductions("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody()).containsEntry("error", "Failed to fetch deductions");
    }

    // ---- /final-account ----

    @Test
    void getFinalAccount_present_returnsMappedAccount() {
        FinalAccount fa = mock(FinalAccount.class);
        when(fa.getId()).thenReturn(99L);
        when(finalAccountRepository.findByProjectId(7L)).thenReturn(Optional.of(fa));

        ResponseEntity<Map<String, Object>> r = controller.getFinalAccount("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsKey("finalAccount");
        Object inner = r.getBody().get("finalAccount");
        assertThat(inner).isInstanceOf(Map.class);
        assertThat((Map<String, Object>) inner).containsEntry("id", 99L);
    }

    @Test
    void getFinalAccount_absent_returnsNullWithMessage() {
        when(finalAccountRepository.findByProjectId(7L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> r = controller.getFinalAccount("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsKey("finalAccount");
        assertThat(r.getBody().get("finalAccount")).isNull();
        assertThat(r.getBody()).containsEntry("message", "Not yet prepared");
    }

    @Test
    void getFinalAccount_repoThrows_returns500() {
        when(finalAccountRepository.findByProjectId(7L)).thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> r = controller.getFinalAccount("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody()).containsEntry("error", "Failed to fetch final account");
    }

    // ---- /boq-invoices ----

    @Test
    void getBoqInvoices_excludesDraft_andReturnsCount() {
        when(boqInvoiceRepository
                .findByProjectIdAndStatusNotOrderByCreatedAtDesc(7L, InvoiceStatus.DRAFT))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> r = controller.getBoqInvoices("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsEntry("count", 0);
        // Must query the DRAFT-excluding method
        verify(boqInvoiceRepository)
                .findByProjectIdAndStatusNotOrderByCreatedAtDesc(7L, InvoiceStatus.DRAFT);
    }

    @Test
    void getBoqInvoices_repoThrows_returns500() {
        when(boqInvoiceRepository
                .findByProjectIdAndStatusNotOrderByCreatedAtDesc(anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> r = controller.getBoqInvoices("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody()).containsEntry("error", "Failed to fetch BOQ invoices");
    }

    // ---- /summary ----

    @Test
    void getSummary_aggregatesAcrossRepositories() {
        PaymentStage paid = mock(PaymentStage.class);
        when(paid.getStatus()).thenReturn(PaymentStageStatus.PAID);
        when(paid.getRetentionHeld()).thenReturn(new BigDecimal("10.00"));
        when(paid.getPaidAmount()).thenReturn(new BigDecimal("500.00"));
        PaymentStage upcoming = mock(PaymentStage.class);
        when(upcoming.getStatus()).thenReturn(PaymentStageStatus.UPCOMING);
        when(upcoming.getRetentionHeld()).thenReturn(null);
        when(upcoming.getPaidAmount()).thenReturn(null);
        when(stageRepository.findByProjectIdOrderByStageNumberAsc(7L))
                .thenReturn(List.of(paid, upcoming));

        ChangeOrder approved = mock(ChangeOrder.class);
        when(approved.getStatus()).thenReturn("APPROVED");
        ChangeOrder draft = mock(ChangeOrder.class);
        when(draft.getStatus()).thenReturn("DRAFT");
        when(changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(approved, draft));

        DeductionRegister pending = mock(DeductionRegister.class);
        when(pending.getDecision()).thenReturn("PENDING");
        when(deductionRepository.findByProjectIdOrderByCreatedAtDesc(7L))
                .thenReturn(List.of(pending));

        FinalAccount fa = mock(FinalAccount.class);
        when(fa.getStatus()).thenReturn("DRAFT");
        when(fa.getBalancePayable()).thenReturn(new BigDecimal("1234.00"));
        when(fa.getRetentionReleased()).thenReturn(Boolean.FALSE);
        when(finalAccountRepository.findByProjectId(7L)).thenReturn(Optional.of(fa));

        ResponseEntity<Map<String, Object>> r = controller.getSummary("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = r.getBody();
        assertThat(body)
                .containsEntry("totalStages", 2)
                .containsEntry("stagesPaid", 1L)
                .containsEntry("totalPaidToDate", new BigDecimal("500.00"))
                .containsEntry("totalRetentionHeld", new BigDecimal("10.00"))
                .containsEntry("approvedVariationOrders", 1L)
                .containsEntry("pendingDeductions", 1L)
                .containsEntry("finalAccountStatus", "DRAFT")
                .containsEntry("balancePayable", new BigDecimal("1234.00"))
                .containsEntry("retentionReleased", Boolean.FALSE);
    }

    @Test
    void getSummary_noFinalAccount_omitsBalanceAndNullStatus() {
        when(stageRepository.findByProjectIdOrderByStageNumberAsc(7L)).thenReturn(List.of());
        when(changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        when(deductionRepository.findByProjectIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        when(finalAccountRepository.findByProjectId(7L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> r = controller.getSummary("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        Map<String, Object> body = r.getBody();
        assertThat(body)
                .containsEntry("totalStages", 0)
                .containsEntry("finalAccountStatus", null)
                .doesNotContainKey("balancePayable");
    }

    @Test
    void getSummary_repoThrows_returns500() {
        when(stageRepository.findByProjectIdOrderByStageNumberAsc(7L))
                .thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> r = controller.getSummary("uuid", auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat(r.getBody()).containsEntry("error", "Failed to build financial summary");
    }
}
