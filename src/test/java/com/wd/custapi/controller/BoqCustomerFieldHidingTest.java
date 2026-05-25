package com.wd.custapi.controller;

import com.wd.custapi.dto.ProjectModuleDtos.*;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * REGRESSION-LOCK — audit Card 4.4 (BOQ read-only, CUSTOMER role field-hiding).
 *
 * <p>Invariant: {@code ProjectModuleController.getBoqItems} must set quantity, unit,
 * rate, amount, executedQuantity, billedQuantity, remainingQuantity,
 * totalExecutedAmount, and totalBilledAmount to {@code null} for CUSTOMER and
 * CUSTOMER_ADMIN roles. These fields represent contractor unit-pricing BoQ takeoff
 * data that is commercially sensitive IP and must never be exposed to the customer.
 *
 * <p>Non-financial fields (description, itemCode, status, executionPercentage,
 * billingPercentage, specifications, notes, itemKind, isActive) must be preserved
 * unchanged.
 *
 * <p>This test characterises the current production behaviour and must NEVER be
 * weakened to accommodate a regression in the field-hiding logic.
 */
@ExtendWith(MockitoExtension.class)
class BoqCustomerFieldHidingTest {

    // ---- Mirror the 16 constructor deps from ProjectModuleQcSnagPermissionTest ----
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

    /** A fully-populated BoqItemDto with ALL financial fields set to non-null values. */
    private BoqItemDto populatedItem;

    @BeforeEach
    void setUp() {
        // Build the project stub leniently — same pattern as QcSnagPermissionTest.
        Project project = new Project();
        project.setId(50L);
        lenient().when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenReturn(project);

        // Build the fully-populated BoqItemDto BEFORE any when() stubs that use it,
        // to avoid the Mockito nested-stubbing gotcha.
        populatedItem = new BoqItemDto(
                1L,                          // id
                50L,                         // projectId
                10L,                         // workTypeId
                "Civil",                     // workTypeName
                20L,                         // categoryId
                "Masonry",                   // categoryName
                "ITEM-001",                  // itemCode
                "Brick wall construction",   // description
                new BigDecimal("100.00"),    // quantity  — MUST be hidden for CUSTOMER
                "m2",                        // unit      — MUST be hidden for CUSTOMER
                new BigDecimal("350.00"),    // rate      — MUST be hidden for CUSTOMER
                new BigDecimal("35000.00"),  // amount    — MUST be hidden for CUSTOMER
                new BigDecimal("60.00"),     // executedQuantity  — MUST be hidden
                new BigDecimal("40.00"),     // billedQuantity    — MUST be hidden
                new BigDecimal("40.00"),     // remainingQuantity — MUST be hidden
                new BigDecimal("21000.00"),  // totalExecutedAmount — MUST be hidden
                new BigDecimal("14000.00"),  // totalBilledAmount   — MUST be hidden
                new BigDecimal("60.00"),     // executionPercentage — preserved
                new BigDecimal("40.00"),     // billingPercentage   — preserved
                "APPROVED",                  // status              — preserved
                "Spec A",                    // specifications      — preserved
                "Note B",                    // notes               — preserved
                LocalDateTime.of(2026, 1, 1, 10, 0), // createdAt
                LocalDateTime.of(2026, 1, 2, 10, 0), // updatedAt
                99L,                         // createdById
                "Builder Admin",             // createdByName
                Boolean.TRUE,               // isActive
                "BASE"                       // itemKind            — preserved
        );
    }

    // -------------------------------------------------------------------------
    // Case 1: CUSTOMER role — all financial fields must be null
    // -------------------------------------------------------------------------

    @Test
    void getBoqItems_customerRole_financialFieldsAreAllNull() {
        String email = "customer@example.com";
        when(auth.getName()).thenReturn(email);
        when(dashboardService.getUserRole(email)).thenReturn("CUSTOMER");
        when(boqService.getProjectBoqItems(50L)).thenReturn(List.of(populatedItem));

        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<List<BoqItemDto>>> response =
                (ResponseEntity<ApiResponse<List<BoqItemDto>>>) (ResponseEntity<?>)
                controller.getBoqItems("proj-50-uuid", null, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        List<BoqItemDto> items = response.getBody().data();
        assertThat(items).hasSize(1);
        BoqItemDto item = items.get(0);

        // --- HIDDEN fields (contractor pricing IP) ---
        assertThat(item.quantity())
                .as("quantity must be null for CUSTOMER")
                .isNull();
        assertThat(item.unit())
                .as("unit must be null for CUSTOMER")
                .isNull();
        assertThat(item.rate())
                .as("rate must be null for CUSTOMER")
                .isNull();
        assertThat(item.amount())
                .as("amount must be null for CUSTOMER")
                .isNull();
        assertThat(item.executedQuantity())
                .as("executedQuantity must be null for CUSTOMER")
                .isNull();
        assertThat(item.billedQuantity())
                .as("billedQuantity must be null for CUSTOMER")
                .isNull();
        assertThat(item.remainingQuantity())
                .as("remainingQuantity must be null for CUSTOMER")
                .isNull();
        assertThat(item.totalExecutedAmount())
                .as("totalExecutedAmount must be null for CUSTOMER")
                .isNull();
        assertThat(item.totalBilledAmount())
                .as("totalBilledAmount must be null for CUSTOMER")
                .isNull();

        // --- PRESERVED fields ---
        assertThat(item.description())
                .as("description must be preserved for CUSTOMER")
                .isEqualTo("Brick wall construction");
        assertThat(item.itemCode())
                .as("itemCode must be preserved for CUSTOMER")
                .isEqualTo("ITEM-001");
        assertThat(item.status())
                .as("status must be preserved for CUSTOMER")
                .isEqualTo("APPROVED");
        assertThat(item.executionPercentage())
                .as("executionPercentage must be preserved for CUSTOMER")
                .isNotNull();
        assertThat(item.itemKind())
                .as("itemKind must be preserved for CUSTOMER")
                .isEqualTo("BASE");
    }

    // -------------------------------------------------------------------------
    // Case 2: CUSTOMER_ADMIN role — same hiding rules apply
    // -------------------------------------------------------------------------

    @Test
    void getBoqItems_customerAdminRole_financialFieldsAreAllNull() {
        String email = "ca@example.com";
        when(auth.getName()).thenReturn(email);
        when(dashboardService.getUserRole(email)).thenReturn("CUSTOMER_ADMIN");
        when(boqService.getProjectBoqItems(50L)).thenReturn(List.of(populatedItem));

        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<List<BoqItemDto>>> response =
                (ResponseEntity<ApiResponse<List<BoqItemDto>>>) (ResponseEntity<?>)
                controller.getBoqItems("proj-50-uuid", null, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        BoqItemDto item = response.getBody().data().get(0);

        assertThat(item.quantity()).isNull();
        assertThat(item.unit()).isNull();
        assertThat(item.rate()).isNull();
        assertThat(item.amount()).isNull();
        assertThat(item.executedQuantity()).isNull();
        assertThat(item.billedQuantity()).isNull();
        assertThat(item.remainingQuantity()).isNull();
        assertThat(item.totalExecutedAmount()).isNull();
        assertThat(item.totalBilledAmount()).isNull();

        // Preserved
        assertThat(item.status()).isEqualTo("APPROVED");
        assertThat(item.executionPercentage()).isNotNull();
    }

    // -------------------------------------------------------------------------
    // Case 3: ADMIN role — financial fields must NOT be hidden (positive control)
    // -------------------------------------------------------------------------

    @Test
    void getBoqItems_adminRole_financialFieldsArePreserved() {
        String email = "admin@walldotbuilders.com";
        when(auth.getName()).thenReturn(email);
        when(dashboardService.getUserRole(email)).thenReturn("ADMIN");
        when(boqService.getProjectBoqItems(50L)).thenReturn(List.of(populatedItem));

        // ADMIN path calls getUserIdFromAuth which resolves via customerUserRepository for
        // non-numeric email. Stub it leniently so the controller can proceed past auth.
        CustomerUser admin = new CustomerUser();
        admin.setId(1L);
        lenient().when(customerUserRepository.findByEmail(email))
                .thenReturn(Optional.of(admin));

        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<List<BoqItemDto>>> response =
                (ResponseEntity<ApiResponse<List<BoqItemDto>>>) (ResponseEntity<?>)
                controller.getBoqItems("proj-50-uuid", null, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        BoqItemDto item = response.getBody().data().get(0);

        // For ADMIN, the controller does NOT apply any field-hiding — all values pass through.
        assertThat(item.quantity())
                .as("quantity must be NON-null for ADMIN (hiding is customer-specific)")
                .isNotNull();
        assertThat(item.rate())
                .as("rate must be NON-null for ADMIN")
                .isNotNull();
        assertThat(item.amount())
                .as("amount must be NON-null for ADMIN")
                .isNotNull();
        assertThat(item.totalExecutedAmount())
                .as("totalExecutedAmount must be NON-null for ADMIN")
                .isNotNull();
    }
}
