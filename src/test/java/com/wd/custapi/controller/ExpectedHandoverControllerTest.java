package com.wd.custapi.controller;

import com.wd.custapi.dto.ExpectedHandoverDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.service.DashboardService;
import com.wd.custapi.service.ExpectedHandoverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExpectedHandoverController. Pattern mirrors
 * CustomerDelayLogControllerTest — pure-mock with @ExtendWith(MockitoExtension).
 *
 * <p>Critical contract guards:
 * <ul>
 *   <li>The controller MUST verify project ownership (via DashboardService)
 *       BEFORE consulting the service so an unauthorised UUID never hits the
 *       cache.</li>
 *   <li>The DTO is returned as-is, with nullable fields preserved.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ExpectedHandoverControllerTest {

    @Mock private ExpectedHandoverService expectedHandoverService;
    @Mock private DashboardService dashboardService;
    @Mock private Authentication auth;

    @InjectMocks
    private ExpectedHandoverController controller;

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(42L);
        when(auth.getName()).thenReturn("alice@x");
        when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenReturn(project);
    }

    @Test
    void get_returnsDtoWhenAuthAndProjectOwnershipPass() {
        ExpectedHandoverDto dto = new ExpectedHandoverDto(
                LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 5),
                14,
                true);
        when(expectedHandoverService.compute("uuid")).thenReturn(dto);

        ResponseEntity<ExpectedHandoverDto> response = controller.get("uuid", auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void get_callsDashboardServiceForOwnershipBeforeService() {
        when(expectedHandoverService.compute("target-uuid"))
                .thenReturn(new ExpectedHandoverDto(null, null, null, false));

        controller.get("target-uuid", auth);

        // Ownership lookup must happen first; service second.
        InOrder ordered = inOrder(dashboardService, expectedHandoverService);
        ordered.verify(dashboardService).getProjectByUuidAndEmail("target-uuid", "alice@x");
        ordered.verify(expectedHandoverService).compute("target-uuid");
    }

    @Test
    void get_returnsNullableFieldsAsIs() {
        ExpectedHandoverDto dto = new ExpectedHandoverDto(null, null, null, false);
        when(expectedHandoverService.compute("uuid")).thenReturn(dto);

        ResponseEntity<ExpectedHandoverDto> response = controller.get("uuid", auth);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().projectFinishDate()).isNull();
        assertThat(response.getBody().baselineFinishDate()).isNull();
        assertThat(response.getBody().weeksRemaining()).isNull();
        assertThat(response.getBody().hasMaterialDelay()).isFalse();
    }
}
