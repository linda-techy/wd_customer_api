package com.wd.custapi.controller;

import com.wd.custapi.model.DelayLog;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.DelayLogRepository;
import com.wd.custapi.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerDelayLogController.
 *
 * The critical regression this test guards against: the controller must call
 * the CUSTOMER-VISIBLE-ONLY repository method, not the raw
 * {@code findByProjectIdOrderByFromDateDesc}. Calling the unfiltered method
 * would leak internal-only delays to customers.
 */
@ExtendWith(MockitoExtension.class)
class CustomerDelayLogControllerTest {

    @Mock private DelayLogRepository delayLogRepository;
    @Mock private DashboardService dashboardService;
    @Mock private Authentication auth;

    @InjectMocks
    private CustomerDelayLogController controller;

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(7L);
        when(auth.getName()).thenReturn("customer@example.com");
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenReturn(project);
    }

    @Test
    void getDelayLogs_callsCustomerVisibleFilteredQuery_notRaw() {
        when(delayLogRepository.findByProjectIdAndCustomerVisibleTrueOrderByFromDateDesc(7L))
                .thenReturn(List.of());

        controller.getDelayLogs("project-uuid", auth);

        // Must use the customer-visible-only query
        verify(delayLogRepository).findByProjectIdAndCustomerVisibleTrueOrderByFromDateDesc(7L);
        // And NEVER fall back to the raw unfiltered query
        verify(delayLogRepository, never()).findByProjectIdOrderByFromDateDesc(anyLong());
    }

    @Test
    void getDelayLogs_returnsDtosMappedFromRepositoryResults() {
        DelayLog d = mock(DelayLog.class);
        when(d.getId()).thenReturn(1L);
        when(d.getDelayType()).thenReturn("WEATHER");
        when(d.getFromDate()).thenReturn(LocalDate.now().minusDays(2));
        when(d.getCustomerSummary()).thenReturn("Heavy rainfall delay");
        when(d.getImpactOnHandover()).thenReturn("NONE");

        when(delayLogRepository.findByProjectIdAndCustomerVisibleTrueOrderByFromDateDesc(7L))
                .thenReturn(List.of(d));

        ResponseEntity<Map<String, Object>> response = controller.getDelayLogs("uuid", auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("count")).isEqualTo(1);
    }

    @Test
    void getDelayLogs_verifiesProjectOwnershipBeforeQuery() {
        when(delayLogRepository.findByProjectIdAndCustomerVisibleTrueOrderByFromDateDesc(7L))
                .thenReturn(List.of());

        controller.getDelayLogs("target-uuid", auth);

        // Ownership check must run before the repo query
        verify(dashboardService).getProjectByUuidAndEmail("target-uuid", "customer@example.com");
    }
}
