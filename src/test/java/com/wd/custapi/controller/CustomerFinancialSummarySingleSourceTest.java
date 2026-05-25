package com.wd.custapi.controller;

import com.wd.custapi.model.ChangeOrder;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.BoqInvoiceRepository;
import com.wd.custapi.repository.ChangeOrderPaymentScheduleRepository;
import com.wd.custapi.repository.ChangeOrderRepository;
import com.wd.custapi.repository.DeductionRegisterRepository;
import com.wd.custapi.repository.FinalAccountRepository;
import com.wd.custapi.repository.PaymentStageRepository;
import com.wd.custapi.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Characterization tests locking the CUSTOMER-side financial summary as single-source.
 *
 * <p>Audit Cards 4.9 (Financials) / 4.6 (Payments): the customer's commercial-change figure
 * ({@code approvedVariationOrders}) is sourced from {@code change_orders} (ChangeOrder) ONLY —
 * consistent with the portal's {@code BoqFinanceDashboardService}. {@code wd_customer_api} has no
 * {@code ProjectVariation} model; the structural guard below prevents one from being wired into
 * customer financials later (which would reintroduce the P1-3 inconsistency).
 */
@ExtendWith(MockitoExtension.class)
class CustomerFinancialSummarySingleSourceTest {

    @Mock DashboardService dashboardService;
    @Mock PaymentStageRepository stageRepository;
    @Mock ChangeOrderRepository changeOrderRepository;
    @Mock ChangeOrderPaymentScheduleRepository scheduleRepository;
    @Mock DeductionRegisterRepository deductionRepository;
    @Mock FinalAccountRepository finalAccountRepository;
    @Mock BoqInvoiceRepository boqInvoiceRepository;
    @Mock Authentication auth;

    @InjectMocks
    CustomerFinancialController controller;

    private ChangeOrder co(String status) {
        ChangeOrder co = mock(ChangeOrder.class);
        lenient().when(co.getStatus()).thenReturn(status);
        return co;
    }

    @Test
    void summary_approvedVariationOrders_countsApprovedChangeOrdersOnly() {
        Project p = new Project();
        p.setId(50L);
        when(auth.getName()).thenReturn("customer@test.com");
        when(dashboardService.getProjectByUuidAndEmail("proj-50-uuid", "customer@test.com")).thenReturn(p);
        // Build the ChangeOrder mocks first — constructing them inside thenReturn(...) would nest
        // stubbing and trigger Mockito's UnfinishedStubbingException.
        ChangeOrder approvedA = co("APPROVED");
        ChangeOrder approvedB = co("APPROVED");
        ChangeOrder draft = co("DRAFT");
        when(stageRepository.findByProjectIdOrderByStageNumberAsc(50L)).thenReturn(List.of());
        when(changeOrderRepository.findByProjectIdOrderByCreatedAtDesc(50L))
                .thenReturn(List.of(approvedA, approvedB, draft));
        when(deductionRepository.findByProjectIdOrderByCreatedAtDesc(50L)).thenReturn(List.of());
        when(finalAccountRepository.findByProjectId(50L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> resp = controller.getSummary("proj-50-uuid", auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        // Only the two APPROVED change orders are counted; the DRAFT is excluded.
        assertThat(resp.getBody().get("approvedVariationOrders")).isEqualTo(2L);
    }

    @Test
    void controller_hasNoProjectVariationDependency() {
        // ProjectVariation is non-financial (owner decision 2026-05-25). If a ProjectVariation-typed
        // repository is ever injected into the customer financial controller, this test fails.
        for (Field f : CustomerFinancialController.class.getDeclaredFields()) {
            assertThat(f.getType().getSimpleName())
                    .as("CustomerFinancialController field %s", f.getName())
                    .doesNotContain("ProjectVariation");
        }
    }
}
