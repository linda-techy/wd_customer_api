package com.wd.custapi.controller;

import com.wd.custapi.dto.CustomerWarrantyDto;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.ProjectWarranty;
import com.wd.custapi.repository.ProjectWarrantyRepository;
import com.wd.custapi.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Characterization / regression-lock tests for CustomerWarrantyController.
 *
 * Audit Card 4.11: Customer warranties are read-only.  Two invariants are locked:
 *
 * 1. The GET handler verifies project ownership (via dashboardService) BEFORE
 *    returning data — preventing IDOR access to another customer's warranties.
 *
 * 2. There is no write path on the controller (no POST / PUT / DELETE / PATCH
 *    mapping) — enforcing "customer warranties are read-only" at the routing layer.
 */
@ExtendWith(MockitoExtension.class)
class CustomerWarrantyControllerTest {

    @Mock private ProjectWarrantyRepository warrantyRepository;
    @Mock private DashboardService dashboardService;
    @Mock private Authentication auth;

    @InjectMocks
    private CustomerWarrantyController controller;

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(50L);
        lenient().when(auth.getName()).thenReturn("customer@example.com");
        lenient().when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenReturn(project);
    }

    // ---------------------------------------------------------------------------
    // Test 1 — ownership check runs before data is returned
    // ---------------------------------------------------------------------------

    @Test
    void getWarranties_verifiesProjectOwnershipBeforeReturningData() {
        when(warrantyRepository.findByProjectIdOrderByEndDateDesc(50L))
                .thenReturn(List.of());

        controller.getWarranties("proj-50-uuid", auth);

        // Ownership must be verified with the caller's email
        verify(dashboardService).getProjectByUuidAndEmail("proj-50-uuid", "customer@example.com");
    }

    @Test
    void getWarranties_returnsOkWithServiceData() {
        ProjectWarranty warranty = mock(ProjectWarranty.class);
        when(warranty.getId()).thenReturn(1L);
        when(warranty.getComponentName()).thenReturn("Roof Warranty");
        when(warranty.getDescription()).thenReturn(null);
        when(warranty.getProviderName()).thenReturn("Vendor A");
        when(warranty.getStartDate()).thenReturn(null);
        when(warranty.getEndDate()).thenReturn(null);
        when(warranty.getStatus()).thenReturn(null);
        when(warranty.getCoverageDetails()).thenReturn(null);
        when(warranty.getCreatedAt()).thenReturn(null);
        when(warrantyRepository.findByProjectIdOrderByEndDateDesc(50L))
                .thenReturn(List.of(warranty));

        ResponseEntity<Map<String, Object>> response =
                controller.getWarranties("proj-50-uuid", auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        List<CustomerWarrantyDto> warranties =
                (List<CustomerWarrantyDto>) response.getBody().get("warranties");
        assertThat(warranties).hasSize(1);
        assertThat(response.getBody().get("count")).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------
    // Test 2 — no write path: every public request-mapping method is a GET
    // ---------------------------------------------------------------------------

    @Test
    void customerWarrantyController_hasNoWriteMappings() {
        List<Method> writeMethods = Arrays.stream(CustomerWarrantyController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PostMapping.class)
                          || m.isAnnotationPresent(PutMapping.class)
                          || m.isAnnotationPresent(DeleteMapping.class)
                          || m.isAnnotationPresent(PatchMapping.class))
                .toList();

        // Any entry in this list means a write endpoint was added — audit violation.
        assertThat(writeMethods)
                .as("CustomerWarrantyController must have no write (POST/PUT/DELETE/PATCH) endpoints")
                .isEmpty();
    }
}
