package com.wd.custapi.controller;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class ProjectModuleController {
    
    private final ProjectDocumentService documentService;
    private final DashboardService dashboardService;
    private final CustomerUserRepository customerUserRepository;
    private final QualityCheckService qualityCheckService;
    private final ActivityFeedService activityFeedService;
    private final GalleryService galleryService;
    private final ObservationService observationService;
    private final ProjectQueryService queryService;
    private final CctvService cctvService;
    private final View360Service view360Service;
    private final SiteVisitService siteVisitService;
    private final FeedbackService feedbackService;
    private final BoqService boqService;

    public ProjectModuleController(ProjectDocumentService documentService,
                                   DashboardService dashboardService,
                                   CustomerUserRepository customerUserRepository,
                                   QualityCheckService qualityCheckService,
                                   ActivityFeedService activityFeedService,
                                   GalleryService galleryService,
                                   ObservationService observationService,
                                   ProjectQueryService queryService,
                                   CctvService cctvService,
                                   View360Service view360Service,
                                   SiteVisitService siteVisitService,
                                   FeedbackService feedbackService,
                                   BoqService boqService) {
        this.documentService = documentService;
        this.dashboardService = dashboardService;
        this.customerUserRepository = customerUserRepository;
        this.qualityCheckService = qualityCheckService;
        this.activityFeedService = activityFeedService;
        this.galleryService = galleryService;
        this.observationService = observationService;
        this.queryService = queryService;
        this.cctvService = cctvService;
        this.view360Service = view360Service;
        this.siteVisitService = siteVisitService;
        this.feedbackService = feedbackService;
        this.boqService = boqService;
    }
    
    // ===== DOCUMENT ENDPOINTS =====
    // Path segment is projectUuid (String); project is resolved for access control.

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProjectDocumentDto>> uploadDocument(
            @PathVariable("projectId") String projectUuid,
            @RequestParam("file") MultipartFile file,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String description,
            Authentication auth) {
        String email = auth.getName();
        Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
        Long userId = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        DocumentUploadRequest request = new DocumentUploadRequest(categoryId, description);
        ProjectDocumentDto doc = documentService.uploadDocument(project.getId(), file, request, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Document uploaded successfully", doc));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<ProjectDocumentDto>>> getDocuments(
            @PathVariable("projectId") String projectUuid,
            @RequestParam(required = false) Long categoryId,
            Authentication auth) {
        String email = auth.getName();
        Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
        List<ProjectDocumentDto> docs = documentService.getProjectDocuments(project.getId(), categoryId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Documents retrieved successfully", docs));
    }

    @GetMapping("/documents/categories")
    public ResponseEntity<ApiResponse<List<DocumentCategoryDto>>> getDocumentCategories(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        String email = auth.getName();
        dashboardService.getProjectByUuidAndEmail(projectUuid, email);
        List<DocumentCategoryDto> categories = documentService.getAllCategories();
        return ResponseEntity.ok(new ApiResponse<>(true, "Categories retrieved successfully", categories));
    }
    
    // ===== QUALITY CHECK ENDPOINTS =====
    
    @PostMapping("/quality-check")
    public ResponseEntity<ApiResponse<QualityCheckDto>> createQualityCheck(
            @PathVariable Long projectId,
            @RequestBody QualityCheckRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        QualityCheckDto qc = qualityCheckService.createQualityCheck(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Quality check created successfully", qc));
    }
    
    @GetMapping("/quality-check")
    public ResponseEntity<ApiResponse<List<QualityCheckDto>>> getQualityChecks(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status) {
        List<QualityCheckDto> checks = qualityCheckService.getQualityChecks(projectId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Quality checks retrieved successfully", checks));
    }
    
    @PutMapping("/quality-check/{qcId}")
    public ResponseEntity<ApiResponse<QualityCheckDto>> resolveQualityCheck(
            @PathVariable Long projectId,
            @PathVariable Long qcId,
            @RequestBody QualityCheckUpdateRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        QualityCheckDto qc = qualityCheckService.resolveQualityCheck(qcId, request, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Quality check resolved successfully", qc));
    }
    
    // ===== ACTIVITY FEED ENDPOINTS =====
    
    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<ActivityFeedDto>>> getActivities(
            @PathVariable Long projectId) {
        List<ActivityFeedDto> activities = activityFeedService.getProjectActivities(projectId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Activities retrieved successfully", activities));
    }
    
    @GetMapping("/activities/range")
    public ResponseEntity<ApiResponse<List<ActivityFeedDto>>> getActivitiesByDateRange(
            @PathVariable Long projectId,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate) {
        List<ActivityFeedDto> activities = activityFeedService.getProjectActivitiesByDateRange(
            projectId, startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(true, "Activities retrieved successfully", activities));
    }
    
    // ===== GALLERY ENDPOINTS =====
    
    @PostMapping(value = "/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GalleryImageDto>> uploadImage(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) LocalDate takenDate,
            @RequestParam(required = false) Long siteReportId,
            @RequestParam(required = false) String locationTag,
            @RequestParam(required = false) List<String> tags,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        GalleryUploadRequest request = new GalleryUploadRequest(caption, takenDate, siteReportId, locationTag, tags);
        GalleryImageDto image = galleryService.uploadImage(projectId, file, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Image uploaded successfully", image));
    }
    
    @GetMapping("/gallery")
    public ResponseEntity<ApiResponse<List<GalleryImageDto>>> getGalleryImages(
            @PathVariable Long projectId,
            @RequestParam(required = false) LocalDate date) {
        List<GalleryImageDto> images;
        if (date != null) {
            images = galleryService.getImagesByDate(projectId, date);
        } else {
            images = galleryService.getProjectImages(projectId);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Gallery images retrieved successfully", images));
    }
    
    // ===== OBSERVATION ENDPOINTS =====
    
    @PostMapping(value = "/observations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ObservationDto>> createObservation(
            @PathVariable Long projectId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) Long reportedByRoleId,
            @RequestParam String priority,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) MultipartFile image,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        ObservationRequest request = new ObservationRequest(title, description, reportedByRoleId, priority, location);
        ObservationDto obs = observationService.createObservation(projectId, request, image, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Observation created successfully", obs));
    }
    
    @GetMapping("/observations")
    public ResponseEntity<ApiResponse<List<ObservationDto>>> getObservations(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status) {
        List<ObservationDto> observations = observationService.getObservations(projectId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Observations retrieved successfully", observations));
    }
    
    @PutMapping("/observations/{obsId}")
    public ResponseEntity<ApiResponse<ObservationDto>> resolveObservation(
            @PathVariable Long projectId,
            @PathVariable Long obsId,
            @RequestBody ObservationResolveRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        ObservationDto obs = observationService.resolveObservation(obsId, request, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Observation resolved successfully", obs));
    }
    
    // ===== QUERY ENDPOINTS =====
    
    @PostMapping("/queries")
    public ResponseEntity<ApiResponse<ProjectQueryDto>> createQuery(
            @PathVariable Long projectId,
            @RequestBody ProjectQueryRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        ProjectQueryDto query = queryService.createQuery(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Query created successfully", query));
    }
    
    @GetMapping("/queries")
    public ResponseEntity<ApiResponse<List<ProjectQueryDto>>> getQueries(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status) {
        List<ProjectQueryDto> queries = queryService.getQueries(projectId, status);
        return ResponseEntity.ok(new ApiResponse<>(true, "Queries retrieved successfully", queries));
    }
    
    @PutMapping("/queries/{queryId}")
    public ResponseEntity<ApiResponse<ProjectQueryDto>> resolveQuery(
            @PathVariable Long projectId,
            @PathVariable Long queryId,
            @RequestBody ProjectQueryResolveRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        ProjectQueryDto query = queryService.resolveQuery(queryId, request, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Query resolved successfully", query));
    }
    
    // ===== CCTV ENDPOINTS =====
    
    @PostMapping("/cctv")
    public ResponseEntity<ApiResponse<CctvCameraDto>> addCamera(
            @PathVariable Long projectId,
            @RequestBody CctvCameraRequest request) {
        CctvCameraDto camera = cctvService.addCamera(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Camera added successfully", camera));
    }
    
    @GetMapping("/cctv")
    public ResponseEntity<ApiResponse<List<CctvCameraDto>>> getCameras(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "false") boolean installedOnly) {
        List<CctvCameraDto> cameras;
        if (installedOnly) {
            cameras = cctvService.getInstalledCameras(projectId);
        } else {
            cameras = cctvService.getProjectCameras(projectId);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Cameras retrieved successfully", cameras));
    }
    
    @PutMapping("/cctv/{cameraId}")
    public ResponseEntity<ApiResponse<CctvCameraDto>> updateCamera(
            @PathVariable Long projectId,
            @PathVariable Long cameraId,
            @RequestBody CctvCameraRequest request) {
        CctvCameraDto camera = cctvService.updateCamera(cameraId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Camera updated successfully", camera));
    }
    
    // ===== 360 VIEW ENDPOINTS =====
    
    @PostMapping("/360-views")
    public ResponseEntity<ApiResponse<View360Dto>> add360View(
            @PathVariable Long projectId,
            @RequestBody View360Request request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        View360Dto view = view360Service.addView360(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "360 view added successfully", view));
    }
    
    @GetMapping("/360-views")
    public ResponseEntity<ApiResponse<List<View360Dto>>> get360Views(
            @PathVariable Long projectId) {
        List<View360Dto> views = view360Service.getProjectViews(projectId);
        return ResponseEntity.ok(new ApiResponse<>(true, "360 views retrieved successfully", views));
    }
    
    @PostMapping("/360-views/{viewId}/increment-count")
    public ResponseEntity<ApiResponse<View360Dto>> incrementViewCount(
            @PathVariable Long projectId,
            @PathVariable Long viewId) {
        View360Dto view = view360Service.incrementViewCount(viewId);
        return ResponseEntity.ok(new ApiResponse<>(true, "View count incremented", view));
    }
    
    // ===== SITE VISIT ENDPOINTS =====
    
    @PostMapping("/site-visits/check-in")
    public ResponseEntity<ApiResponse<SiteVisitDto>> checkIn(
            @PathVariable Long projectId,
            @RequestBody SiteVisitCheckInRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        SiteVisitDto visit = siteVisitService.checkIn(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Checked in successfully", visit));
    }
    
    @PutMapping("/site-visits/{visitId}/check-out")
    public ResponseEntity<ApiResponse<SiteVisitDto>> checkOut(
            @PathVariable Long projectId,
            @PathVariable Long visitId,
            @RequestBody SiteVisitCheckOutRequest request) {
        SiteVisitDto visit = siteVisitService.checkOut(visitId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Checked out successfully", visit));
    }
    
    @GetMapping("/site-visits")
    public ResponseEntity<ApiResponse<List<SiteVisitDto>>> getSiteVisits(
            @PathVariable Long projectId) {
        List<SiteVisitDto> visits = siteVisitService.getProjectVisits(projectId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Site visits retrieved successfully", visits));
    }
    
    // ===== FEEDBACK ENDPOINTS =====
    
    @PostMapping("/feedback/forms")
    public ResponseEntity<ApiResponse<FeedbackFormDto>> createFeedbackForm(
            @PathVariable Long projectId,
            @RequestBody FeedbackFormRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        FeedbackFormDto form = feedbackService.createForm(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Feedback form created successfully", form));
    }
    
    @GetMapping("/feedback")
    public ResponseEntity<ApiResponse<List<FeedbackFormDto>>> getFeedbackForms(
            @PathVariable Long projectId,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        List<FeedbackFormDto> forms = feedbackService.getProjectForms(projectId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Feedback forms retrieved successfully", forms));
    }
    
    @PostMapping("/feedback/{formId}/responses")
    public ResponseEntity<ApiResponse<FeedbackResponseDto>> submitFeedback(
            @PathVariable Long projectId,
            @PathVariable Long formId,
            @RequestBody FeedbackResponseRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        FeedbackResponseDto response = feedbackService.submitResponse(formId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "Feedback submitted successfully", response));
    }
    
    @GetMapping("/feedback/{formId}/responses")
    public ResponseEntity<ApiResponse<List<FeedbackResponseDto>>> getFeedbackResponses(
            @PathVariable Long projectId,
            @PathVariable Long formId) {
        List<FeedbackResponseDto> responses = feedbackService.getFormResponses(formId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Responses retrieved successfully", responses));
    }
    
    // ===== BOQ ENDPOINTS =====
    
    @PostMapping("/boq")
    public ResponseEntity<ApiResponse<BoqItemDto>> addBoqItem(
            @PathVariable Long projectId,
            @RequestBody BoqItemRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        BoqItemDto item = boqService.addBoqItem(projectId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ApiResponse<>(true, "BoQ item added successfully", item));
    }
    
    @PutMapping("/boq/{itemId}")
    public ResponseEntity<ApiResponse<BoqItemDto>> updateBoqItem(
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @RequestBody BoqItemRequest request,
            Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        BoqItemDto item = boqService.updateBoqItem(itemId, request, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, "BoQ item updated successfully", item));
    }
    
    @GetMapping("/boq")
    public ResponseEntity<ApiResponse<List<BoqItemDto>>> getBoqItems(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long workTypeId) {
        List<BoqItemDto> items;
        if (workTypeId != null) {
            items = boqService.getBoqItemsByWorkType(projectId, workTypeId);
        } else {
            items = boqService.getProjectBoqItems(projectId);
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "BoQ items retrieved successfully", items));
    }
    
    @GetMapping("/boq/summary")
    public ResponseEntity<ApiResponse<BoqSummaryDto>> getBoqSummary(
            @PathVariable Long projectId) {
        BoqSummaryDto summary = boqService.getBoqSummary(projectId);
        return ResponseEntity.ok(new ApiResponse<>(true, "BoQ summary retrieved successfully", summary));
    }
    
    @GetMapping("/boq/work-types")
    public ResponseEntity<ApiResponse<List<BoqWorkTypeDto>>> getWorkTypes() {
        List<BoqWorkTypeDto> workTypes = boqService.getAllWorkTypes();
        return ResponseEntity.ok(new ApiResponse<>(true, "Work types retrieved successfully", workTypes));
    }
    
    // Helper method to extract user ID from authentication
    private Long getUserIdFromAuth(Authentication auth) {
        return Long.parseLong(auth.getName());
    }
}

