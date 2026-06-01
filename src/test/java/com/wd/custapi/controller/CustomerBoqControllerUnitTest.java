package com.wd.custapi.controller;

import com.wd.custapi.controller.CustomerBoqController.RejectCoRequest;
import com.wd.custapi.model.BoqDocument;
import com.wd.custapi.model.ChangeOrder;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.PaymentStage;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.enums.BoqDocumentStatus;
import com.wd.custapi.repository.BoqDocumentRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.PaymentStageRepository;
import com.wd.custapi.service.CustomerChangeOrderService;
import com.wd.custapi.service.CustomerNextPaymentService;
import com.wd.custapi.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Direct-invocation Mockito unit tests for {@link CustomerBoqController}.
 *
 * <p>Complements {@link CustomerBoqControllerNextOnlyTest} (which covers only
 * the {@code nextOnly} branch of getPaymentSchedule). This class covers the
 * remaining endpoints: BOQ document/summary/payment-stages, acknowledge,
 * change-order list / pending / detail / approve / reject — happy paths plus the
 * 403/404/400/500 branches.
 *
 * <p>All six constructor collaborators and {@link Authentication} are mocked.
 * No Spring / MockMvc / DB.
 */
@ExtendWith(MockitoExtension.class)
class CustomerBoqControllerUnitTest {

    private static final String UUID = "11111111-1111-1111-1111-111111111111";
    private static final String EMAIL = "bob@test.com";

    @Mock private DashboardService dashboardService;
    @Mock private BoqDocumentRepository boqDocumentRepository;
    @Mock private PaymentStageRepository paymentStageRepository;
    @Mock private CustomerChangeOrderService changeOrderService;
    @Mock private CustomerUserRepository customerUserRepository;
    @Mock private CustomerNextPaymentService nextPaymentService;
    @Mock private Authentication auth;

    private CustomerBoqController controller;
    private Project project;

    @BeforeEach
    void setUp() {
        controller = new CustomerBoqController(
                dashboardService, boqDocumentRepository, paymentStageRepository,
                changeOrderService, customerUserRepository, nextPaymentService);
        project = new Project();
        project.setId(7L);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenReturn(project);
    }

    private BoqDocument docMock(Long id, BoqDocumentStatus status, LocalDateTime ackAt) {
        BoqDocument d = mock(BoqDocument.class);
        lenient().when(d.getId()).thenReturn(id);
        lenient().when(d.getStatus()).thenReturn(status);
        lenient().when(d.getCustomerAcknowledgedAt()).thenReturn(ackAt);
        lenient().when(d.getProject()).thenReturn(project);
        return d;
    }

    // ---- GET /document ----

    @Test
    void getBoqDocument_found_returns200WithDocMap() {
        BoqDocument d = docMock(3L, BoqDocumentStatus.APPROVED, null);
        when(boqDocumentRepository.findTopByProjectIdOrderByRevisionNumberDesc(7L))
                .thenReturn(Optional.of(d));

        ResponseEntity<Map<String, Object>> response = controller.getBoqDocument(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("id", 3L);
        assertThat(response.getBody()).containsEntry("status", BoqDocumentStatus.APPROVED);
    }

    @Test
    void getBoqDocument_none_returns404() {
        when(boqDocumentRepository.findTopByProjectIdOrderByRevisionNumberDesc(7L))
                .thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getBoqDocument(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("success", false);
    }

    @Test
    void getBoqDocument_ownershipThrows_returns500() {
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenThrow(new RuntimeException("no access"));

        ResponseEntity<Map<String, Object>> response = controller.getBoqDocument(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("message", "An internal error occurred");
    }

    // ---- GET /summary ----

    @Test
    void getBoqSummary_approvedDoc_returnsData() {
        BoqDocument d = docMock(3L, BoqDocumentStatus.APPROVED, LocalDateTime.now());
        when(boqDocumentRepository.findByProjectIdAndStatus(7L, BoqDocumentStatus.APPROVED))
                .thenReturn(Optional.of(d));
        when(paymentStageRepository.findByBoqDocumentIdOrderByStageNumberAsc(3L))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.getBoqSummary(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("success", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        // approved + acknowledged => not pending acknowledgement
        assertThat(data)
                .containsEntry("documentId", 3L)
                .containsEntry("pendingAcknowledgement", false);
    }

    @Test
    void getBoqSummary_fallsBackToLatestNonRejected() {
        BoqDocument d = docMock(4L, BoqDocumentStatus.PENDING_APPROVAL, null);
        when(boqDocumentRepository.findByProjectIdAndStatus(7L, BoqDocumentStatus.APPROVED))
                .thenReturn(Optional.empty());
        when(boqDocumentRepository.findTopByProjectIdAndStatusNotOrderByRevisionNumberDesc(
                7L, BoqDocumentStatus.REJECTED)).thenReturn(Optional.of(d));
        when(paymentStageRepository.findByBoqDocumentIdOrderByStageNumberAsc(4L))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.getBoqSummary(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        assertThat(data).containsEntry("documentId", 4L);
    }

    @Test
    void getBoqSummary_noDoc_returnsOkNullData() {
        when(boqDocumentRepository.findByProjectIdAndStatus(7L, BoqDocumentStatus.APPROVED))
                .thenReturn(Optional.empty());
        when(boqDocumentRepository.findTopByProjectIdAndStatusNotOrderByRevisionNumberDesc(
                7L, BoqDocumentStatus.REJECTED)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getBoqSummary(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("success", true);
        assertThat(response.getBody()).containsEntry("message", "No BOQ available yet");
        assertThat(response.getBody()).containsKey("data");
        assertThat(response.getBody().get("data")).isNull();
    }

    @Test
    void getBoqSummary_throws_returns500() {
        when(boqDocumentRepository.findByProjectIdAndStatus(any(), any()))
                .thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> response = controller.getBoqSummary(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ---- GET /payment-stages ----

    @Test
    void getBoqPaymentStages_returnsLeanStages() {
        PaymentStage s = mock(PaymentStage.class);
        when(paymentStageRepository.findByProjectIdOrderByStageNumberAsc(7L))
                .thenReturn(List.of(s));

        ResponseEntity<Map<String, Object>> response = controller.getBoqPaymentStages(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("success", true);
        assertThat((List<?>) response.getBody().get("stages")).hasSize(1);
    }

    @Test
    void getBoqPaymentStages_throws_returns500() {
        when(paymentStageRepository.findByProjectIdOrderByStageNumberAsc(7L))
                .thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> response = controller.getBoqPaymentStages(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ---- PATCH /documents/{id}/acknowledge ----

    @Test
    void acknowledgeBoq_firstTime_recordsAndReturnsData() {
        BoqDocument d = docMock(8L, BoqDocumentStatus.APPROVED, null);
        BoqDocument reloaded = docMock(8L, BoqDocumentStatus.APPROVED, LocalDateTime.now());
        when(boqDocumentRepository.findById(8L))
                .thenReturn(Optional.of(d))   // first lookup
                .thenReturn(Optional.of(reloaded)); // reload after update

        CustomerUser cu = new CustomerUser();
        cu.setId(24L);
        when(customerUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(cu));
        when(paymentStageRepository.findByBoqDocumentIdOrderByStageNumberAsc(8L))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.acknowledgeBoq(UUID, 8L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("message", "BOQ acknowledged");
        verify(boqDocumentRepository).recordAcknowledgement(eq(8L), any(LocalDateTime.class), eq(24L));
    }

    @Test
    void acknowledgeBoq_alreadyAcknowledged_isIdempotentNoWrite() {
        BoqDocument d = docMock(8L, BoqDocumentStatus.APPROVED, LocalDateTime.now());
        when(boqDocumentRepository.findById(8L)).thenReturn(Optional.of(d));
        when(paymentStageRepository.findByBoqDocumentIdOrderByStageNumberAsc(8L))
                .thenReturn(List.of());

        ResponseEntity<Map<String, Object>> response = controller.acknowledgeBoq(UUID, 8L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("message", "BOQ acknowledged");
        verify(boqDocumentRepository, never()).recordAcknowledgement(any(), any(), any());
    }

    @Test
    void acknowledgeBoq_documentBelongsToOtherProject_returns403() {
        Project other = new Project();
        other.setId(99L);
        BoqDocument d = mock(BoqDocument.class);
        when(d.getProject()).thenReturn(other);
        when(boqDocumentRepository.findById(8L)).thenReturn(Optional.of(d));

        ResponseEntity<Map<String, Object>> response = controller.acknowledgeBoq(UUID, 8L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).containsEntry("message", "Document does not belong to this project");
        verify(boqDocumentRepository, never()).recordAcknowledgement(any(), any(), any());
    }

    @Test
    void acknowledgeBoq_documentNotFound_returns404() {
        when(boqDocumentRepository.findById(8L)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.acknowledgeBoq(UUID, 8L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("success", false);
    }

    @Test
    void acknowledgeBoq_unexpected_returns500() {
        when(boqDocumentRepository.findById(8L)).thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> response = controller.acknowledgeBoq(UUID, 8L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ---- GET /change-orders ----

    @Test
    void getChangeOrders_returnsList() {
        ChangeOrder co = mock(ChangeOrder.class);
        when(changeOrderService.getProjectChangeOrders(7L)).thenReturn(List.of(co));

        ResponseEntity<Map<String, Object>> response = controller.getChangeOrders(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) response.getBody().get("changeOrders")).hasSize(1);
    }

    @Test
    void getChangeOrders_throws_returns500() {
        when(changeOrderService.getProjectChangeOrders(7L)).thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> response = controller.getChangeOrders(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ---- GET /change-orders/pending-review ----

    @Test
    void getPendingReview_returnsList() {
        ChangeOrder co = mock(ChangeOrder.class);
        when(changeOrderService.getPendingReview(7L)).thenReturn(List.of(co));

        ResponseEntity<Map<String, Object>> response = controller.getPendingReview(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) response.getBody().get("changeOrders")).hasSize(1);
    }

    @Test
    void getPendingReview_throws_returns500() {
        when(changeOrderService.getPendingReview(7L)).thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> response = controller.getPendingReview(UUID, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ---- GET /change-orders/{coId} ----

    @Test
    void getChangeOrder_found_returns200() {
        ChangeOrder co = mock(ChangeOrder.class);
        when(co.getId()).thenReturn(20L);
        when(changeOrderService.getChangeOrder(20L, 7L)).thenReturn(co);

        ResponseEntity<Map<String, Object>> response = controller.getChangeOrder(UUID, 20L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("success", true);
        @SuppressWarnings("unchecked")
        Map<String, Object> coMap = (Map<String, Object>) response.getBody().get("changeOrder");
        assertThat(coMap).containsEntry("id", 20L);
    }

    @Test
    void getChangeOrder_notFound_returns404() {
        when(changeOrderService.getChangeOrder(20L, 7L))
                .thenThrow(new IllegalArgumentException("Change order not found: 20"));

        ResponseEntity<Map<String, Object>> response = controller.getChangeOrder(UUID, 20L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).containsEntry("message", "Change order not found: 20");
    }

    @Test
    void getChangeOrder_unexpected_returns500() {
        when(changeOrderService.getChangeOrder(20L, 7L)).thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> response = controller.getChangeOrder(UUID, 20L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ---- PATCH /change-orders/{coId}/approve ----

    @Test
    void approveChangeOrder_success_returns200() {
        ChangeOrder co = mock(ChangeOrder.class);
        when(co.getId()).thenReturn(20L);
        when(changeOrderService.approve(20L, 7L, EMAIL)).thenReturn(co);

        ResponseEntity<Map<String, Object>> response = controller.approveChangeOrder(UUID, 20L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("message", "Change order approved successfully");
    }

    @Test
    void approveChangeOrder_illegalState_returns400() {
        when(changeOrderService.approve(20L, 7L, EMAIL))
                .thenThrow(new IllegalStateException("must be in CUSTOMER_REVIEW"));

        ResponseEntity<Map<String, Object>> response = controller.approveChangeOrder(UUID, 20L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("message", "must be in CUSTOMER_REVIEW");
    }

    @Test
    void approveChangeOrder_unexpected_returns500() {
        when(changeOrderService.approve(20L, 7L, EMAIL)).thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> response = controller.approveChangeOrder(UUID, 20L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    // ---- PATCH /change-orders/{coId}/reject ----

    @Test
    void rejectChangeOrder_success_returns200() {
        ChangeOrder co = mock(ChangeOrder.class);
        when(co.getId()).thenReturn(20L);
        when(changeOrderService.reject(20L, 7L, EMAIL, "too costly")).thenReturn(co);

        ResponseEntity<Map<String, Object>> response =
                controller.rejectChangeOrder(UUID, 20L, new RejectCoRequest("too costly"), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("message", "Change order rejected");
    }

    @Test
    void rejectChangeOrder_illegalArgument_returns400() {
        when(changeOrderService.reject(20L, 7L, EMAIL, "x"))
                .thenThrow(new IllegalArgumentException("Customer not found: bob@test.com"));

        ResponseEntity<Map<String, Object>> response =
                controller.rejectChangeOrder(UUID, 20L, new RejectCoRequest("x"), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("success", false);
    }

    @Test
    void rejectChangeOrder_unexpected_returns500() {
        when(changeOrderService.reject(20L, 7L, EMAIL, "x")).thenThrow(new RuntimeException("db"));

        ResponseEntity<Map<String, Object>> response =
                controller.rejectChangeOrder(UUID, 20L, new RejectCoRequest("x"), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }
}
