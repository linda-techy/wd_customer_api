package com.wd.custapi.controller;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.BoqApproval;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.Task;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Direct-invocation unit tests for {@link ProjectModuleController}.
 *
 * No Spring context / MockMvc / DB — every collaborator is a Mockito mock and the
 * controller methods are called directly, asserting on the returned ResponseEntity
 * (status + ApiResponse body). Covers happy paths, validation (400), the
 * canAccessFeature role gate (allowed + denied), and the mapped RuntimeException
 * branches (404 user-not-found / 404 resource-not-found / 403 access-denied / 500).
 */
@ExtendWith(MockitoExtension.class)
class ProjectModuleControllerTest {

    @Mock private ProjectDocumentService documentService;
    @Mock private DashboardService dashboardService;
    @Mock private CustomerUserRepository customerUserRepository;
    @Mock private QualityCheckService qualityCheckService;
    @Mock private ActivityFeedService activityFeedService;
    @Mock private GalleryService galleryService;
    @Mock private ObservationService observationService;
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

    private static final String UUID = "proj-50-uuid";
    private static final String EMAIL = "customer@example.com";

    private Project project;
    private CustomerUser user;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(50L);

        user = new CustomerUser();
        user.setId(7L);
        user.setEmail(EMAIL);

        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(dashboardService.getProjectByUuidAndEmail(anyString(), anyString()))
                .thenReturn(project);
        lenient().when(customerUserRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        // Default role: CUSTOMER (read access; no write/snag/qc-resolve access)
        lenient().when(dashboardService.getUserRole(EMAIL)).thenReturn("CUSTOMER");
    }

    // helper for role override
    private void roleIs(String role) {
        when(dashboardService.getUserRole(EMAIL)).thenReturn(role);
    }

    // ===================================================================
    // GANTT
    // ===================================================================

    @Test
    void getGanttData_happyPath_aggregatesTasksAndReturnsOk() {
        Task done = mock(Task.class);
        when(done.getStatus()).thenReturn("COMPLETED");
        when(done.getStartDate()).thenReturn(LocalDate.of(2026, 1, 1));
        when(done.getEndDate()).thenReturn(LocalDate.of(2026, 2, 1));
        when(done.getProgressPercent()).thenReturn(100);

        Task overdue = mock(Task.class);
        when(overdue.getStatus()).thenReturn("IN_PROGRESS");
        when(overdue.getStartDate()).thenReturn(LocalDate.of(2026, 3, 1));
        when(overdue.getEndDate()).thenReturn(LocalDate.now().minusDays(5)); // past -> overdue
        when(overdue.getProgressPercent()).thenReturn(40);

        when(taskRepository.findByProjectIdOrderedForGantt(50L))
                .thenReturn(List.of(done, overdue));

        ResponseEntity<ApiResponse<Map<String, Object>>> resp = controller.getGanttData(UUID, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().success()).isTrue();
        Map<String, Object> data = resp.getBody().data();
        assertThat(data).containsKeys("tasks", "projectStartDate", "projectEndDate",
                "overallProgress", "overdueTasks");
        assertThat((List<?>) data.get("tasks")).hasSize(2);
        // (100 + 40) / 2 = 70
        assertThat(data)
                .containsEntry("overdueTasks", 1)
                .containsEntry("overallProgress", 70)
                .containsEntry("projectStartDate", LocalDate.of(2026, 1, 1));
    }

    @Test
    void getGanttData_emptyTasks_returnsZeroProgress() {
        when(taskRepository.findByProjectIdOrderedForGantt(50L)).thenReturn(List.of());

        ResponseEntity<ApiResponse<Map<String, Object>>> resp = controller.getGanttData(UUID, auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data())
                .containsEntry("overallProgress", 0)
                .containsEntry("overdueTasks", 0);
    }

    @Test
    void getGanttData_projectNotFound_maps404() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("Project not found"));

        ResponseEntity<ApiResponse<Map<String, Object>>> resp = controller.getGanttData(UUID, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().success()).isFalse();
    }

    @Test
    void getGanttData_userNotFound_maps404WithGenericMessage() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("User not found for email: x"));

        ResponseEntity<ApiResponse<Map<String, Object>>> resp = controller.getGanttData(UUID, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().message()).isEqualTo("User account not found");
    }

    @Test
    void getGanttData_accessDenied_maps403() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("Access denied"));

        ResponseEntity<ApiResponse<Map<String, Object>>> resp = controller.getGanttData(UUID, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    void getGanttData_otherRuntime_maps500() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<ApiResponse<Map<String, Object>>> resp = controller.getGanttData(UUID, auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===================================================================
    // DOCUMENTS
    // ===================================================================

    private MultipartFile validFile() {
        return new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});
    }

    @Test
    void uploadDocument_happyPath_returnsOk() {
        ProjectDocumentDto dto = mock(ProjectDocumentDto.class);
        when(documentService.uploadDocument(eq(50L), any(), any(DocumentUploadRequest.class), eq(7L)))
                .thenReturn(dto);

        ResponseEntity<ApiResponse<ProjectDocumentDto>> resp =
                controller.uploadDocument(UUID, validFile(), 3L, "desc", auth);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).isSameAs(dto);
    }

    @Test
    void uploadDocument_emptyFile_returns400() {
        MultipartFile empty = new MockMultipartFile("file", new byte[0]);

        ResponseEntity<ApiResponse<ProjectDocumentDto>> resp =
                controller.uploadDocument(UUID, empty, 3L, "desc", auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().success()).isFalse();
        verifyNoInteractions(documentService);
    }

    @Test
    void uploadDocument_nullFile_returns400() {
        ResponseEntity<ApiResponse<ProjectDocumentDto>> resp =
                controller.uploadDocument(UUID, null, 3L, "desc", auth);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadDocument_serviceThrowsChecked_returns500() {
        when(documentService.uploadDocument(anyLong(), any(), any(), anyLong()))
                .thenThrow(new IllegalStateException("io error")); // RuntimeException -> handler
        ResponseEntity<ApiResponse<ProjectDocumentDto>> resp =
                controller.uploadDocument(UUID, validFile(), 3L, "desc", auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getDocuments_happyPath_returnsOk() {
        when(documentService.getProjectDocuments(50L, null)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<ProjectDocumentDto>>> resp =
                controller.getDocuments(UUID, null, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).isEmpty();
    }

    @Test
    void getDocuments_projectNotFound_maps404() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("Project not found"));
        ResponseEntity<ApiResponse<List<ProjectDocumentDto>>> resp =
                controller.getDocuments(UUID, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getDocumentCategories_happyPath_returnsOk() {
        when(documentService.getAllCategories()).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<DocumentCategoryDto>>> resp =
                controller.getDocumentCategories(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(documentService).getAllCategories();
    }

    // ===================================================================
    // QUALITY CHECK
    // ===================================================================

    @Test
    void getQualityChecks_allowedRole_returnsServiceData() {
        roleIs("ADMIN");
        when(qualityCheckService.getQualityChecks(50L, "OPEN")).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<QualityCheckDto>>> resp =
                controller.getQualityChecks(UUID, "OPEN", auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(qualityCheckService).getQualityChecks(50L, "OPEN");
    }

    @Test
    void getQualityChecks_deniedRole_returnsEmptyListNotForbidden() {
        roleIs("INTERIOR_DESIGNER"); // not in READ_ACCESS_ROLES
        ResponseEntity<ApiResponse<List<QualityCheckDto>>> resp =
                controller.getQualityChecks(UUID, null, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).isEmpty();
        verifyNoInteractions(qualityCheckService);
    }

    @Test
    void getQualityChecks_serviceThrows_returns500() {
        roleIs("ADMIN");
        when(qualityCheckService.getQualityChecks(anyLong(), any()))
                .thenThrow(new RuntimeException("db down"));
        ResponseEntity<ApiResponse<List<QualityCheckDto>>> resp =
                controller.getQualityChecks(UUID, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void resolveQualityCheck_allowedRole_returnsOk() {
        roleIs("SITE_ENGINEER");
        QualityCheckDto dto = mock(QualityCheckDto.class);
        when(qualityCheckService.resolveQualityCheck(eq(9L), any(), eq(7L))).thenReturn(dto);
        ResponseEntity<ApiResponse<QualityCheckDto>> resp =
                controller.resolveQualityCheck(UUID, 9L, new QualityCheckUpdateRequest("ok"), auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).isSameAs(dto);
    }

    @Test
    void resolveQualityCheck_customerRole_returns403() {
        roleIs("CUSTOMER"); // read-only for QC writes
        ResponseEntity<ApiResponse<QualityCheckDto>> resp =
                controller.resolveQualityCheck(UUID, 9L, new QualityCheckUpdateRequest("ok"), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(qualityCheckService, never()).resolveQualityCheck(anyLong(), any(), anyLong());
    }

    @Test
    void resolveQualityCheck_serviceThrows_returns500() {
        roleIs("ADMIN");
        when(qualityCheckService.resolveQualityCheck(anyLong(), any(), anyLong()))
                .thenThrow(new RuntimeException("fail"));
        ResponseEntity<ApiResponse<QualityCheckDto>> resp =
                controller.resolveQualityCheck(UUID, 9L, new QualityCheckUpdateRequest("ok"), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===================================================================
    // ACTIVITIES
    // ===================================================================

    @Test
    void getActivities_happyPath_returnsOk() {
        when(activityFeedService.getProjectActivities(50L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<ActivityFeedDto>>> resp = controller.getActivities(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getActivities_notFound_maps404() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("Project not found"));
        ResponseEntity<ApiResponse<List<ActivityFeedDto>>> resp = controller.getActivities(UUID, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getActivitiesByDateRange_happyPath_returnsOk() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 2, 1, 0, 0);
        when(activityFeedService.getProjectActivitiesByDateRange(50L, start, end))
                .thenReturn(List.of());
        ResponseEntity<ApiResponse<List<ActivityFeedDto>>> resp =
                controller.getActivitiesByDateRange(UUID, start, end, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getActivitiesByDateRange_runtimeError_maps404() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("not found"));
        ResponseEntity<ApiResponse<List<ActivityFeedDto>>> resp =
                controller.getActivitiesByDateRange(UUID, LocalDateTime.now(), LocalDateTime.now(), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCombinedActivityFeed_happyPath_returnsOk() {
        when(activityFeedService.getCombinedActivityFeedByType(50L, "SNAG")).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<ActivityFeedService.CombinedActivityItem>>> resp =
                controller.getCombinedActivityFeed(UUID, "SNAG", auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getCombinedActivityFeed_runtimeError_returns500() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("kaboom"));
        ResponseEntity<ApiResponse<List<ActivityFeedService.CombinedActivityItem>>> resp =
                controller.getCombinedActivityFeed(UUID, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getCombinedActivityFeedGrouped_happyPath_returnsOk() {
        when(activityFeedService.getCombinedActivityFeedGroupedByDate(50L)).thenReturn(Map.of());
        ResponseEntity<ApiResponse<Map<LocalDate, List<ActivityFeedService.CombinedActivityItem>>>> resp =
                controller.getCombinedActivityFeedGrouped(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getCombinedActivityFeedGrouped_runtimeError_returns500() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("kaboom"));
        ResponseEntity<ApiResponse<Map<LocalDate, List<ActivityFeedService.CombinedActivityItem>>>> resp =
                controller.getCombinedActivityFeedGrouped(UUID, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===================================================================
    // GALLERY
    // ===================================================================

    @Test
    void uploadImage_happyPath_returns201() {
        GalleryImageDto dto = mock(GalleryImageDto.class);
        when(galleryService.uploadImage(eq(50L), any(), any(GalleryUploadRequest.class), eq(7L)))
                .thenReturn(dto);
        ResponseEntity<ApiResponse<GalleryImageDto>> resp =
                controller.uploadImage(UUID, validFile(), "cap", LocalDate.now(), 1L, "loc",
                        List.of("a"), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().data()).isSameAs(dto);
    }

    @Test
    void uploadImage_emptyFile_returns400() {
        ResponseEntity<ApiResponse<GalleryImageDto>> resp =
                controller.uploadImage(UUID, new MockMultipartFile("file", new byte[0]),
                        null, null, null, null, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(galleryService);
    }

    @Test
    void getGalleryImages_noDate_usesProjectImages() {
        when(galleryService.getProjectImages(50L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<GalleryImageDto>>> resp =
                controller.getGalleryImages(UUID, null, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(galleryService).getProjectImages(50L);
        verify(galleryService, never()).getImagesByDate(anyLong(), any());
    }

    @Test
    void getGalleryImages_withDate_usesByDate() {
        LocalDate d = LocalDate.of(2026, 5, 1);
        when(galleryService.getImagesByDate(50L, d)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<GalleryImageDto>>> resp =
                controller.getGalleryImages(UUID, d, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(galleryService).getImagesByDate(50L, d);
    }

    @Test
    void getGalleryImagesGrouped_happyPath_returnsOk() {
        when(galleryService.getImagesGroupedByDate(50L)).thenReturn(Map.of());
        ResponseEntity<ApiResponse<Map<LocalDate, List<GalleryImageDto>>>> resp =
                controller.getGalleryImagesGrouped(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    // ===================================================================
    // OBSERVATIONS (SNAGS)
    // ===================================================================

    @Test
    void createObservation_allowedRole_returns201() {
        roleIs("ADMIN");
        ObservationDto dto = mock(ObservationDto.class);
        when(observationService.createObservation(eq(50L), any(), any(), eq(7L))).thenReturn(dto);
        ResponseEntity<ApiResponse<ObservationDto>> resp =
                controller.createObservation(UUID, "t", "d", 1L, "HIGH", "loc", null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createObservation_blankTitle_returns400() {
        ResponseEntity<ApiResponse<ObservationDto>> resp =
                controller.createObservation(UUID, "  ", "d", null, "HIGH", null, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(observationService);
    }

    @Test
    void createObservation_blankDescription_returns400() {
        ResponseEntity<ApiResponse<ObservationDto>> resp =
                controller.createObservation(UUID, "t", "", null, "HIGH", null, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createObservation_blankPriority_returns400() {
        ResponseEntity<ApiResponse<ObservationDto>> resp =
                controller.createObservation(UUID, "t", "d", null, "  ", null, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createObservation_customerRole_returns403() {
        roleIs("CUSTOMER"); // snags read-only for customer
        ResponseEntity<ApiResponse<ObservationDto>> resp =
                controller.createObservation(UUID, "t", "d", null, "HIGH", null, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(observationService);
    }

    @Test
    void getObservations_allowedRole_returnsData() {
        roleIs("CUSTOMER");
        when(observationService.getObservations(50L, "OPEN")).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<ObservationDto>>> resp =
                controller.getObservations(UUID, "OPEN", auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(observationService).getObservations(50L, "OPEN");
    }

    @Test
    void getObservations_deniedRole_returnsEmptyList() {
        roleIs("VIEWER");
        ResponseEntity<ApiResponse<List<ObservationDto>>> resp =
                controller.getObservations(UUID, null, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).isEmpty();
        verifyNoInteractions(observationService);
    }

    @Test
    void getActiveObservations_allowedRole_returnsData() {
        roleIs("ADMIN");
        when(observationService.getActiveObservations(50L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<ObservationDto>>> resp =
                controller.getActiveObservations(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getActiveObservations_deniedRole_returnsEmptyList() {
        roleIs("VIEWER");
        ResponseEntity<ApiResponse<List<ObservationDto>>> resp =
                controller.getActiveObservations(UUID, auth);
        assertThat(resp.getBody().data()).isEmpty();
        verifyNoInteractions(observationService);
    }

    @Test
    void getResolvedObservations_allowedRole_returnsData() {
        roleIs("ADMIN");
        when(observationService.getResolvedObservations(50L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<ObservationDto>>> resp =
                controller.getResolvedObservations(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getResolvedObservations_deniedRole_returnsEmptyList() {
        roleIs("VIEWER");
        ResponseEntity<ApiResponse<List<ObservationDto>>> resp =
                controller.getResolvedObservations(UUID, auth);
        assertThat(resp.getBody().data()).isEmpty();
    }

    @Test
    void getObservationCounts_allowedRole_returnsData() {
        roleIs("ADMIN");
        when(observationService.getObservationCounts(50L)).thenReturn(Map.of("OPEN", 3L));
        ResponseEntity<ApiResponse<Map<String, Long>>> resp =
                controller.getObservationCounts(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).containsEntry("OPEN", 3L);
    }

    @Test
    void getObservationCounts_deniedRole_returnsEmptyMap() {
        roleIs("VIEWER");
        ResponseEntity<ApiResponse<Map<String, Long>>> resp =
                controller.getObservationCounts(UUID, auth);
        assertThat(resp.getBody().data()).isEmpty();
        verifyNoInteractions(observationService);
    }

    @Test
    void resolveObservation_allowedRole_returnsOk() {
        roleIs("SITE_ENGINEER");
        ObservationDto dto = mock(ObservationDto.class);
        when(observationService.resolveObservation(eq(50L), eq(4L), any(), eq(7L))).thenReturn(dto);
        ResponseEntity<ApiResponse<ObservationDto>> resp =
                controller.resolveObservation(UUID, 4L, new ObservationResolveRequest("fixed"), auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void resolveObservation_customerRole_returns403() {
        roleIs("CUSTOMER");
        ResponseEntity<ApiResponse<ObservationDto>> resp =
                controller.resolveObservation(UUID, 4L, new ObservationResolveRequest("fixed"), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(observationService);
    }

    @Test
    void resolveObservation_serviceThrowsNotFound_maps404() {
        roleIs("ADMIN");
        when(observationService.resolveObservation(anyLong(), anyLong(), any(), anyLong()))
                .thenThrow(new RuntimeException("Observation not found"));
        ResponseEntity<ApiResponse<ObservationDto>> resp =
                controller.resolveObservation(UUID, 4L, new ObservationResolveRequest("fixed"), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ===================================================================
    // CCTV
    // ===================================================================

    @Test
    void getCameras_allowedRole_returnsData() {
        roleIs("CUSTOMER");
        when(cctvService.getActiveCameras(50L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<CctvCameraDto>>> resp = controller.getCameras(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(cctvService).getActiveCameras(50L);
    }

    @Test
    void getCameras_deniedRole_returnsEmptyList() {
        roleIs("SITE_ENGINEER"); // not in CCTV allowed set
        ResponseEntity<ApiResponse<List<CctvCameraDto>>> resp = controller.getCameras(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).isEmpty();
        verifyNoInteractions(cctvService);
    }

    @Test
    void getCameras_serviceThrows_returns500() {
        roleIs("ADMIN");
        when(cctvService.getActiveCameras(50L)).thenThrow(new RuntimeException("err"));
        ResponseEntity<ApiResponse<List<CctvCameraDto>>> resp = controller.getCameras(UUID, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===================================================================
    // 360 VIEWS
    // ===================================================================

    @Test
    void add360View_happyPath_returns201() {
        View360Dto dto = mock(View360Dto.class);
        when(view360Service.addView360(eq(50L), any(), eq(7L))).thenReturn(dto);
        View360Request req = new View360Request("t", "d", "url", "thumb", LocalDate.now(), "loc");
        ResponseEntity<ApiResponse<View360Dto>> resp = controller.add360View(UUID, req, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void add360View_serviceThrows_returns500() {
        when(view360Service.addView360(anyLong(), any(), anyLong()))
                .thenThrow(new RuntimeException("err"));
        View360Request req = new View360Request("t", "d", "url", "thumb", LocalDate.now(), "loc");
        ResponseEntity<ApiResponse<View360Dto>> resp = controller.add360View(UUID, req, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void get360Views_happyPath_returnsOk() {
        when(view360Service.getProjectViews(50L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<View360Dto>>> resp = controller.get360Views(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void incrementViewCount_happyPath_returnsOk() {
        View360Dto dto = mock(View360Dto.class);
        when(view360Service.incrementViewCount(8L, 50L)).thenReturn(dto);
        ResponseEntity<ApiResponse<View360Dto>> resp = controller.incrementViewCount(UUID, 8L, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).isSameAs(dto);
    }

    @Test
    void incrementViewCount_serviceThrows_returns500() {
        when(view360Service.incrementViewCount(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("err"));
        ResponseEntity<ApiResponse<View360Dto>> resp = controller.incrementViewCount(UUID, 8L, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ===================================================================
    // SITE VISITS
    // ===================================================================

    @Test
    void checkIn_happyPath_returns201() {
        SiteVisitDto dto = mock(SiteVisitDto.class);
        when(siteVisitService.checkIn(eq(50L), any(), eq(7L))).thenReturn(dto);
        SiteVisitCheckInRequest req =
                new SiteVisitCheckInRequest(1L, "purpose", "loc", "sunny", List.of(), 1.0, 2.0);
        ResponseEntity<ApiResponse<SiteVisitDto>> resp = controller.checkIn(UUID, req, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void checkIn_runtimeError_maps404() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("Project not found"));
        SiteVisitCheckInRequest req =
                new SiteVisitCheckInRequest(1L, "p", "l", "w", List.of(), 1.0, 2.0);
        ResponseEntity<ApiResponse<SiteVisitDto>> resp = controller.checkIn(UUID, req, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void checkOut_happyPath_returnsOk() {
        SiteVisitDto dto = mock(SiteVisitDto.class);
        when(siteVisitService.checkOut(eq(11L), any())).thenReturn(dto);
        SiteVisitCheckOutRequest req = new SiteVisitCheckOutRequest("notes", "findings", 1.0, 2.0);
        ResponseEntity<ApiResponse<SiteVisitDto>> resp = controller.checkOut(UUID, 11L, req, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void checkOut_serviceThrowsNotFound_maps404() {
        when(siteVisitService.checkOut(anyLong(), any()))
                .thenThrow(new RuntimeException("Visit not found"));
        SiteVisitCheckOutRequest req = new SiteVisitCheckOutRequest("n", "f", 1.0, 2.0);
        ResponseEntity<ApiResponse<SiteVisitDto>> resp = controller.checkOut(UUID, 11L, req, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getSiteVisits_happyPath_returnsOk() {
        when(siteVisitService.getProjectVisits(50L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<SiteVisitDto>>> resp = controller.getSiteVisits(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getCompletedSiteVisits_happyPath_returnsOk() {
        when(siteVisitService.getCompletedVisits(50L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<SiteVisitDto>>> resp =
                controller.getCompletedSiteVisits(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getOngoingSiteVisits_happyPath_returnsOk() {
        when(siteVisitService.getOngoingVisits(50L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<SiteVisitDto>>> resp =
                controller.getOngoingSiteVisits(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getOngoingSiteVisits_runtimeError_maps404() {
        when(dashboardService.getProjectByUuidAndEmail(UUID, EMAIL))
                .thenThrow(new RuntimeException("not found"));
        ResponseEntity<ApiResponse<List<SiteVisitDto>>> resp =
                controller.getOngoingSiteVisits(UUID, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ===================================================================
    // FEEDBACK
    // ===================================================================

    @Test
    void createFeedbackForm_happyPath_returns201() {
        FeedbackFormDto dto = mock(FeedbackFormDto.class);
        when(feedbackService.createForm(eq(50L), any(), eq(7L))).thenReturn(dto);
        ResponseEntity<ApiResponse<FeedbackFormDto>> resp =
                controller.createFeedbackForm(UUID, new FeedbackFormRequest("t", "d", "GENERAL"), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void createFeedbackForm_serviceThrows_returns500() {
        when(feedbackService.createForm(anyLong(), any(), anyLong()))
                .thenThrow(new RuntimeException("err"));
        ResponseEntity<ApiResponse<FeedbackFormDto>> resp =
                controller.createFeedbackForm(UUID, new FeedbackFormRequest("t", "d", "G"), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getFeedbackForms_happyPath_returnsOk() {
        when(feedbackService.getProjectForms(50L, 7L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<FeedbackFormDto>>> resp =
                controller.getFeedbackForms(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void submitFeedback_happyPath_returns201() {
        FeedbackResponseDto dto = mock(FeedbackResponseDto.class);
        when(feedbackService.submitResponse(eq(2L), any(), eq(7L))).thenReturn(dto);
        ResponseEntity<ApiResponse<FeedbackResponseDto>> resp =
                controller.submitFeedback(UUID, 2L,
                        new FeedbackResponseRequest(5, "great", Map.of()), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void submitFeedback_serviceThrows_returns500() {
        when(feedbackService.submitResponse(anyLong(), any(), anyLong()))
                .thenThrow(new RuntimeException("err"));
        ResponseEntity<ApiResponse<FeedbackResponseDto>> resp =
                controller.submitFeedback(UUID, 2L, new FeedbackResponseRequest(5, "x", Map.of()), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getFeedbackResponses_happyPath_returnsOk() {
        when(feedbackService.getFormResponsesForCustomer(2L, 7L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<FeedbackResponseDto>>> resp =
                controller.getFeedbackResponses(UUID, 2L, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    // ===================================================================
    // BOQ
    // ===================================================================

    private BoqItemDto boqItem(String status) {
        return new BoqItemDto(
                1L, 50L, 2L, "Civil", 3L, "Cat", "IC-1", "desc",
                new BigDecimal("10"), "sqm", new BigDecimal("100"), new BigDecimal("1000"),
                new BigDecimal("5"), new BigDecimal("4"), new BigDecimal("5"),
                new BigDecimal("500"), new BigDecimal("400"),
                new BigDecimal("50"), new BigDecimal("40"),
                status, "specs", "notes",
                LocalDateTime.now(), LocalDateTime.now(), 9L, "Creator", true, "BASE");
    }

    @Test
    void getBoqItems_siteEngineer_returnsEmptyList() {
        roleIs("SITE_ENGINEER");
        ResponseEntity<ApiResponse<List<BoqItemDto>>> resp = controller.getBoqItems(UUID, null, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).isEmpty();
        verifyNoInteractions(boqService);
    }

    @Test
    void getBoqItems_admin_returnsAllFieldsIncludingDraft() {
        roleIs("ADMIN");
        when(boqService.getProjectBoqItems(50L))
                .thenReturn(List.of(boqItem("DRAFT"), boqItem("APPROVED")));
        ResponseEntity<ApiResponse<List<BoqItemDto>>> resp = controller.getBoqItems(UUID, null, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        // ADMIN: no filtering, financial fields intact
        assertThat(resp.getBody().data()).hasSize(2);
        assertThat(resp.getBody().data().get(0).rate()).isEqualByComparingTo("100");
    }

    @Test
    void getBoqItems_customer_seesDraftButFinancialFieldsHidden() {
        roleIs("CUSTOMER");
        when(boqService.getProjectBoqItems(50L))
                .thenReturn(List.of(boqItem("DRAFT"), boqItem("APPROVED")));
        ResponseEntity<ApiResponse<List<BoqItemDto>>> resp = controller.getBoqItems(UUID, null, auth);
        assertThat(resp.getBody().data()).hasSize(2); // DRAFT retained for customer
        BoqItemDto first = resp.getBody().data().get(0);
        assertThat(first.quantity()).isNull();
        assertThat(first.rate()).isNull();
        assertThat(first.amount()).isNull();
        assertThat(first.executionPercentage()).isEqualByComparingTo("50"); // progress kept
    }

    @Test
    void getBoqItems_architect_hidesDraftAndFinancials() {
        roleIs("ARCHITECT");
        when(boqService.getProjectBoqItems(50L))
                .thenReturn(List.of(boqItem("DRAFT"), boqItem("APPROVED")));
        ResponseEntity<ApiResponse<List<BoqItemDto>>> resp = controller.getBoqItems(UUID, null, auth);
        // DRAFT filtered out -> 1 item
        assertThat(resp.getBody().data()).hasSize(1);
        BoqItemDto item = resp.getBody().data().get(0);
        assertThat(item.quantity()).isEqualByComparingTo("10"); // qty visible
        assertThat(item.rate()).isNull();                       // rate hidden
        assertThat(item.amount()).isNull();
        assertThat(item.billingPercentage()).isNull();
    }

    @Test
    void getBoqItems_withWorkTypeId_usesByWorkType() {
        roleIs("ADMIN");
        when(boqService.getBoqItemsByWorkType(50L, 99L)).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<BoqItemDto>>> resp = controller.getBoqItems(UUID, 99L, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        verify(boqService).getBoqItemsByWorkType(50L, 99L);
    }

    @Test
    void getBoqItems_serviceThrows_returns500() {
        roleIs("ADMIN");
        when(boqService.getProjectBoqItems(50L)).thenThrow(new RuntimeException("err"));
        ResponseEntity<ApiResponse<List<BoqItemDto>>> resp = controller.getBoqItems(UUID, null, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getWorkTypes_happyPath_returnsOk() {
        when(boqService.getAllWorkTypes()).thenReturn(List.of());
        ResponseEntity<ApiResponse<List<BoqWorkTypeDto>>> resp = controller.getWorkTypes();
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void getWorkTypes_serviceThrows_returns500() {
        when(boqService.getAllWorkTypes()).thenThrow(new RuntimeException("err"));
        ResponseEntity<ApiResponse<List<BoqWorkTypeDto>>> resp = controller.getWorkTypes();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void submitBoqApproval_customerApproved_returnsOkAndSaves() {
        roleIs("CUSTOMER");
        ResponseEntity<ApiResponse<Map<String, String>>> resp =
                controller.submitBoqApproval(UUID, new BoqApprovalRequest("approved", "looks good"), auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).containsEntry("status", "APPROVED");
        verify(boqApprovalRepository).save(any(BoqApproval.class));
        verify(notificationTriggerService)
                .notifyBoqApprovalAction(user, project, "APPROVED", "looks good");
    }

    @Test
    void submitBoqApproval_nonCustomerRole_returns403() {
        roleIs("ADMIN");
        ResponseEntity<ApiResponse<Map<String, String>>> resp =
                controller.submitBoqApproval(UUID, new BoqApprovalRequest("APPROVED", null), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(boqApprovalRepository);
    }

    @Test
    void submitBoqApproval_invalidStatus_returns400() {
        roleIs("CUSTOMER");
        ResponseEntity<ApiResponse<Map<String, String>>> resp =
                controller.submitBoqApproval(UUID, new BoqApprovalRequest("MAYBE", null), auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(boqApprovalRepository);
    }

    @Test
    void submitBoqApproval_userNotFound_returns500() {
        roleIs("CUSTOMER");
        when(customerUserRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        ResponseEntity<ApiResponse<Map<String, String>>> resp =
                controller.submitBoqApproval(UUID,
                        new BoqApprovalRequest("CHANGE_REQUESTED", "redo"), auth);
        // RuntimeException caught by generic catch -> 500
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getBoqApprovalStatus_noRecord_returnsPending() {
        when(boqApprovalRepository.findTopByProjectIdOrderByCreatedAtDesc(50L))
                .thenReturn(Optional.empty());
        ResponseEntity<ApiResponse<Map<String, String>>> resp =
                controller.getBoqApprovalStatus(UUID, auth);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().data()).containsEntry("status", "PENDING");
    }

    @Test
    void getBoqApprovalStatus_withRecord_returnsLatest() {
        BoqApproval a = new BoqApproval(50L, 7L, "APPROVED", "ok");
        when(boqApprovalRepository.findTopByProjectIdOrderByCreatedAtDesc(50L))
                .thenReturn(Optional.of(a));
        ResponseEntity<ApiResponse<Map<String, String>>> resp =
                controller.getBoqApprovalStatus(UUID, auth);
        assertThat(resp.getBody().data()).containsEntry("status", "APPROVED");
        assertThat(resp.getBody().data()).containsEntry("message", "ok");
    }

    @Test
    void getBoqApprovalStatus_repoThrows_returns500() {
        when(boqApprovalRepository.findTopByProjectIdOrderByCreatedAtDesc(50L))
                .thenThrow(new RuntimeException("db"));
        ResponseEntity<ApiResponse<Map<String, String>>> resp =
                controller.getBoqApprovalStatus(UUID, auth);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
