package com.wd.custapi.controller;

import com.wd.custapi.dto.NextPaymentMilestoneDto;
import com.wd.custapi.model.Project;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomerBoqController.getPaymentSchedule's nextOnly query-param branch.
 *
 * <p>Pattern mirrors ExpectedHandoverControllerTest / CustomerDelayLogControllerTest
 * — pure-mock with {@code @ExtendWith(MockitoExtension)}, no Spring context.
 *
 * <p>Critical contract guards:
 * <ul>
 *   <li>{@code nextOnly=true} delegates to {@link CustomerNextPaymentService} and
 *       MUST NOT touch {@link PaymentStageRepository#findByProjectIdOrderByStageNumberAsc}.</li>
 *   <li>The all-terminal case (service returns dto with {@code stage=null}) is
 *       still HTTP 200 with the DTO body.</li>
 *   <li>{@code nextOnly} absent / false delegates to the existing full-schedule
 *       path which DOES read PaymentStageRepository.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class CustomerBoqControllerNextOnlyTest {

    @Mock private DashboardService dashboardService;
    @Mock private BoqDocumentRepository boqDocumentRepository;
    @Mock private PaymentStageRepository paymentStageRepository;
    @Mock private CustomerChangeOrderService changeOrderService;
    @Mock private CustomerUserRepository customerUserRepository;
    @Mock private CustomerNextPaymentService nextPaymentService;
    @Mock private Authentication auth;

    private CustomerBoqController controller;
    private Project project;

    private static final String UUID = "11111111-1111-1111-1111-111111111111";

    @BeforeEach
    void setUp() {
        controller = new CustomerBoqController(
                dashboardService, boqDocumentRepository, paymentStageRepository,
                changeOrderService, customerUserRepository, nextPaymentService);
        project = new Project();
        project.setId(7L);
        when(auth.getName()).thenReturn("bob@test.com");
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString())).thenReturn(project);
    }

    @Test
    void nextOnlyTrue_routesToNextPaymentService() {
        NextPaymentMilestoneDto dto = new NextPaymentMilestoneDto(
                new NextPaymentMilestoneDto.Stage(
                        4, "Plastering", LocalDate.of(2026, 5, 15), 5,
                        "DUE", new BigDecimal("425000"),
                        new BigDecimal("12"), new BigDecimal("12.0"), 7),
                new NextPaymentMilestoneDto.Summary(
                        new BigDecimal("3500000"), new BigDecimal("1400000"),
                        new BigDecimal("2100000"), 7));
        when(nextPaymentService.getNextPaymentMilestone(project)).thenReturn(dto);

        ResponseEntity<?> response = controller.getPaymentSchedule(UUID, true, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(dto);

        // Crucially, the existing schedule path is NOT exercised in nextOnly mode.
        verify(paymentStageRepository, never()).findByProjectIdOrderByStageNumberAsc(any());
    }

    @Test
    void nextOnlyTrue_allTerminalReturnsNullStage() {
        NextPaymentMilestoneDto dto = new NextPaymentMilestoneDto(
                null,
                new NextPaymentMilestoneDto.Summary(
                        new BigDecimal("3500000"), new BigDecimal("3500000"),
                        BigDecimal.ZERO, 7));
        when(nextPaymentService.getNextPaymentMilestone(project)).thenReturn(dto);

        ResponseEntity<?> response = controller.getPaymentSchedule(UUID, true, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(dto);
        assertThat(((NextPaymentMilestoneDto) response.getBody()).stage()).isNull();
        verify(paymentStageRepository, never()).findByProjectIdOrderByStageNumberAsc(any());
    }

    @Test
    void nextOnlyAbsent_returnsExistingFullSchedule() {
        when(paymentStageRepository.findByProjectIdOrderByStageNumberAsc(7L))
                .thenReturn(Collections.emptyList());

        ResponseEntity<?> response = controller.getPaymentSchedule(UUID, false, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        // Existing shape: { success: true, stages: [], summary: {...} }
        assertThat(response.getBody()).isInstanceOf(java.util.Map.class);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> body = (java.util.Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("success", true);
        assertThat(body.get("stages")).isInstanceOf(java.util.List.class);
        // The full-schedule path DID read the repository.
        verify(paymentStageRepository).findByProjectIdOrderByStageNumberAsc(7L);
        // And the next-payment service was NOT called.
        verify(nextPaymentService, never()).getNextPaymentMilestone(any());
    }
}
