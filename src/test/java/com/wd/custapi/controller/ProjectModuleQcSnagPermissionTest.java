package com.wd.custapi.controller;

import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.BoqApprovalRepository;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.TaskRepository;
import com.wd.custapi.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Permission tests for the customer-API QC and Observation (snag) WRITE endpoints.
 * Authoritative rule (audit charter Cards 4.3 / 4.2): the customer app is read-only
 * for Quality Checks and Snags. A CUSTOMER / CUSTOMER_ADMIN token must receive 403
 * on any QC/observation write; reads stay open.
 */
@ExtendWith(MockitoExtension.class)
class ProjectModuleQcSnagPermissionTest {

    @Mock private ProjectDocumentService documentService;
    @Mock private DashboardService dashboardService;
    @Mock private CustomerUserRepository customerUserRepository;
    @Mock private QualityCheckService qualityCheckService;
    @Mock private ActivityFeedService activityFeedService;
    @Mock private GalleryService galleryService;
    @Mock private ObservationService observationService;
    @Mock private ProjectQueryService queryService;
    @Mock private CctvService cctvService;
    @Mock private View360Service view360Service;
    @Mock private SiteVisitService siteVisitService;
    @Mock private FeedbackService feedbackService;
    @Mock private BoqService boqService;
    @Mock private BoqApprovalRepository boqApprovalRepository;
    @Mock private NotificationTriggerService notificationTriggerService;
    @Mock private TaskRepository taskRepository;

    @Mock private Authentication auth;

    @InjectMocks
    private ProjectModuleController controller;

    private void asRole(String email, String role) {
        when(auth.getName()).thenReturn(email);
        when(dashboardService.getUserRole(email)).thenReturn(role);
    }

    @BeforeEach
    void setUp() {
        // Project lookup only happens on the allowed path; stub leniently so it never
        // breaks a forbidden-path test where it is not reached.
        Project project = new Project();
        project.setId(50L);
        lenient().when(dashboardService.getProjectByUuidAndEmail(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(project);
    }

    @Test
    void resolveQualityCheck_customerRole_isForbidden() {
        asRole("customer@test.com", "CUSTOMER");

        ResponseEntity<?> resp = controller.resolveQualityCheck("proj-50-uuid", 7L, null, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void resolveQualityCheck_customerAdminRole_isForbidden() {
        asRole("ca@test.com", "CUSTOMER_ADMIN");

        ResponseEntity<?> resp = controller.resolveQualityCheck("proj-50-uuid", 7L, null, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void resolveQualityCheck_adminRole_isNotForbiddenByRoleGate() {
        asRole("admin@walldotbuilders.com", "ADMIN");
        // ADMIN is non-numeric email -> getUserIdFromAuth resolves via repository.
        CustomerUser admin = new CustomerUser();
        admin.setId(1L);
        lenient().when(customerUserRepository.findByEmail("admin@walldotbuilders.com"))
                .thenReturn(Optional.of(admin));

        ResponseEntity<?> resp = controller.resolveQualityCheck("proj-50-uuid", 7L, null, auth);

        // The role gate must let ADMIN through. Downstream may return 200/404/500
        // depending on unstubbed services, but it must NOT be the 403 from the gate.
        assertThat(resp.getStatusCode().value()).isNotEqualTo(403);
    }

    @Test
    void createObservation_customerRole_isForbidden() {
        asRole("customer@test.com", "CUSTOMER");

        ResponseEntity<?> resp = controller.createObservation(
                "proj-50-uuid", "Crack in wall", "Hairline crack near window",
                null, "HIGH", null, null, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void createObservation_adminRole_isNotForbiddenByRoleGate() {
        asRole("admin@walldotbuilders.com", "ADMIN");

        ResponseEntity<?> resp = controller.createObservation(
                "proj-50-uuid", "Crack in wall", "Hairline crack near window",
                null, "HIGH", null, null, auth);

        assertThat(resp.getStatusCode().value()).isNotEqualTo(403);
    }

    @Test
    void resolveObservation_customerRole_isForbidden() {
        asRole("customer@test.com", "CUSTOMER");

        ResponseEntity<?> resp = controller.resolveObservation("proj-50-uuid", 9L, null, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void resolveObservation_customerAdminRole_isForbidden() {
        asRole("ca@test.com", "CUSTOMER_ADMIN");

        ResponseEntity<?> resp = controller.resolveObservation("proj-50-uuid", 9L, null, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void resolveObservation_adminRole_isNotForbiddenByRoleGate() {
        asRole("admin@walldotbuilders.com", "ADMIN");
        // ADMIN is non-numeric email -> getUserIdFromAuth resolves via repository.
        CustomerUser admin = new CustomerUser();
        admin.setId(1L);
        lenient().when(customerUserRepository.findByEmail("admin@walldotbuilders.com"))
                .thenReturn(Optional.of(admin));

        ResponseEntity<?> resp = controller.resolveObservation("proj-50-uuid", 9L, null, auth);

        // The role gate must let ADMIN through. Downstream may return 200/404/500
        // depending on unstubbed services, but it must NOT be the 403 from the gate.
        assertThat(resp.getStatusCode().value()).isNotEqualTo(403);
    }

    @Test
    void getQualityChecks_customerRole_isNotForbidden() {
        asRole("customer@test.com", "CUSTOMER");
        lenient().when(qualityCheckService.getQualityChecks(50L, null))
                .thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.getQualityChecks("proj-50-uuid", null, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getObservations_customerRole_isNotForbidden() {
        asRole("customer@test.com", "CUSTOMER");
        lenient().when(observationService.getObservations(50L, null))
                .thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.getObservations("proj-50-uuid", null, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }
}
