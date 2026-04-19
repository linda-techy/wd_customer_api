package com.wd.custapi.service;

import com.wd.custapi.controller.CustomerPaymentController;
import com.wd.custapi.dto.CustomerPaymentScheduleDto;
import com.wd.custapi.dto.ProjectModuleDtos.ApiResponse;
import com.wd.custapi.model.PaymentSchedule;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.PaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerPaymentController — covers payment retrieval and access control.
 * PaymentSchedule has no setters (read-only entity), so it is mocked so its getters
 * can be stubbed for the CustomerPaymentScheduleDto constructor.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceTest {

    @Mock
    private PaymentScheduleRepository paymentScheduleRepository;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private CustomerPaymentController paymentController;

    private Authentication auth;
    private Project project;
    private PaymentSchedule schedule;  // mocked — no setters on the entity

    @BeforeEach
    void setUp() {
        auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("john@example.com");

        project = new Project();
        project.setId(5L);
        project.setName("Test Project");

        // PaymentSchedule is a read-only entity with no setters; mock it so we can stub getters
        schedule = mock(PaymentSchedule.class);
        when(schedule.getId()).thenReturn(100L);
        when(schedule.getInstallmentNumber()).thenReturn(1);
        when(schedule.getDescription()).thenReturn("Foundation payment");
        when(schedule.getAmount()).thenReturn(BigDecimal.valueOf(100000));
        when(schedule.getStatus()).thenReturn("PENDING");
        when(schedule.getDueDate()).thenReturn(LocalDate.now().plusDays(30));
        when(schedule.getPaidAmount()).thenReturn(BigDecimal.ZERO);
        when(schedule.getPaidDate()).thenReturn(null);
        when(schedule.getTransactions()).thenReturn(new ArrayList<>());
    }

    // ── getCustomerPayments ───────────────────────────────────────────────────

    @Test
    void getCustomerPayments_returnsPagedList() {
        when(dashboardService.getUserRole("john@example.com")).thenReturn("CUSTOMER");
        when(dashboardService.getProjectsForUser("john@example.com")).thenReturn(List.of(project));

        Page<PaymentSchedule> page = new PageImpl<>(List.of(schedule));
        when(paymentScheduleRepository.findByProjectIdIn(eq(List.of(5L)), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Page<CustomerPaymentScheduleDto>>> response =
                paymentController.getCustomerPayments(null, 0, 20, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        Page<CustomerPaymentScheduleDto> data = response.getBody().data();
        assertNotNull(data);
        assertEquals(1, data.getTotalElements());
        assertEquals("Foundation payment", data.getContent().get(0).getDescription());
    }

    @Test
    void getCustomerPayments_filtersByProjectId() {
        when(dashboardService.getUserRole("john@example.com")).thenReturn("CUSTOMER");
        when(dashboardService.getProjectsForUser("john@example.com")).thenReturn(List.of(project));

        Page<PaymentSchedule> page = new PageImpl<>(List.of(schedule));
        when(paymentScheduleRepository.findByProjectIdIn(eq(List.of(5L)), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Page<CustomerPaymentScheduleDto>>> response =
                paymentController.getCustomerPayments(5L, 0, 20, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(paymentScheduleRepository).findByProjectIdIn(eq(List.of(5L)), any(Pageable.class));
    }

    @Test
    void getCustomerPayments_unauthorizedProjectId_returns403() {
        when(dashboardService.getUserRole("john@example.com")).thenReturn("CUSTOMER");
        // User only has project 5; requesting project 99
        when(dashboardService.getProjectsForUser("john@example.com")).thenReturn(List.of(project));

        ResponseEntity<ApiResponse<Page<CustomerPaymentScheduleDto>>> response =
                paymentController.getCustomerPayments(99L, 0, 20, auth);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(paymentScheduleRepository, never()).findByProjectIdIn(any(), any());
    }

    // ── getPaymentScheduleById ────────────────────────────────────────────────

    @Test
    void getPaymentScheduleById_nonexistentId_returns500WithError() {
        when(dashboardService.getUserRole("john@example.com")).thenReturn("CUSTOMER");
        when(paymentScheduleRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<CustomerPaymentScheduleDto>> response =
                paymentController.getPaymentScheduleById(999L, auth);

        // findById throws RuntimeException via orElseThrow — controller catches and returns 500
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
    }
}
