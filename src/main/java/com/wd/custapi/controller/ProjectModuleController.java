package com.wd.custapi.controller;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
public class ProjectModuleController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectModuleController.class);

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

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProjectDocumentDto>> uploadDocument(
            @PathVariable("projectId") String projectUuid,
            @RequestParam("file") MultipartFile file,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String description,
            Authentication auth) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "File is required and cannot be empty", null));
            }
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            Long userId = resolveUserId(email);
            DocumentUploadRequest request = new DocumentUploadRequest(categoryId, description);
            ProjectDocumentDto doc = documentService.uploadDocument(project.getId(), file, request, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Document uploaded successfully", doc));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "upload document", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to upload document for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to upload document", null));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<ProjectDocumentDto>>> getDocuments(
            @PathVariable("projectId") String projectUuid,
            @RequestParam(required = false) Long categoryId,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<ProjectDocumentDto> docs = documentService.getProjectDocuments(project.getId(), categoryId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Documents retrieved successfully", docs));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get documents", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get documents for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve documents", null));
        }
    }

    @GetMapping("/documents/categories")
    public ResponseEntity<ApiResponse<List<DocumentCategoryDto>>> getDocumentCategories(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<DocumentCategoryDto> categories = documentService.getAllCategories();
            return ResponseEntity.ok(new ApiResponse<>(true, "Categories retrieved successfully", categories));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get document categories", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get document categories for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve categories", null));
        }
    }

    // ===== QUALITY CHECK ENDPOINTS =====

    @PostMapping("/quality-check")
    public ResponseEntity<ApiResponse<QualityCheckDto>> createQualityCheck(
            @PathVariable Long projectId,
            @RequestBody QualityCheckRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            QualityCheckDto qc = qualityCheckService.createQualityCheck(projectId, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Quality check created successfully", qc));
        } catch (Exception e) {
            logger.error("Failed to create quality check for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to create quality check", null));
        }
    }

    @GetMapping("/quality-check")
    public ResponseEntity<ApiResponse<List<QualityCheckDto>>> getQualityChecks(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status) {
        try {
            List<QualityCheckDto> checks = qualityCheckService.getQualityChecks(projectId, status);
            return ResponseEntity.ok(new ApiResponse<>(true, "Quality checks retrieved successfully", checks));
        } catch (Exception e) {
            logger.error("Failed to get quality checks for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve quality checks", null));
        }
    }

    @PutMapping("/quality-check/{qcId}")
    public ResponseEntity<ApiResponse<QualityCheckDto>> resolveQualityCheck(
            @PathVariable Long projectId,
            @PathVariable Long qcId,
            @RequestBody QualityCheckUpdateRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            QualityCheckDto qc = qualityCheckService.resolveQualityCheck(qcId, request, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Quality check resolved successfully", qc));
        } catch (Exception e) {
            logger.error("Failed to resolve quality check {} for project {}: {}", qcId, projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to resolve quality check", null));
        }
    }

    // ===== ACTIVITY FEED ENDPOINTS =====

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<ActivityFeedDto>>> getActivities(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<ActivityFeedDto> activities = activityFeedService.getProjectActivities(project.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Activities retrieved successfully", activities));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get activities", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get activities for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve activities", null));
        }
    }

    @GetMapping("/activities/range")
    public ResponseEntity<ApiResponse<List<ActivityFeedDto>>> getActivitiesByDateRange(
            @PathVariable("projectId") String projectUuid,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<ActivityFeedDto> activities = activityFeedService.getProjectActivitiesByDateRange(
                project.getId(), startDate, endDate);
            return ResponseEntity.ok(new ApiResponse<>(true, "Activities retrieved successfully", activities));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get activities by range", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get activities by date range for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve activities", null));
        }
    }

    /**
     * Combined activity feed with site reports and queries.
     * Returns a chronological timeline for display.
     */
    @GetMapping("/activities/combined")
    public ResponseEntity<ApiResponse<List<ActivityFeedService.CombinedActivityItem>>> getCombinedActivityFeed(
            @PathVariable("projectId") String projectUuid,
            @RequestParam(required = false) String type,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<ActivityFeedService.CombinedActivityItem> activities =
                activityFeedService.getCombinedActivityFeedByType(project.getId(), type);
            return ResponseEntity.ok(new ApiResponse<>(true, "Combined activities retrieved successfully", activities));
        } catch (RuntimeException e) {
            logger.error("Runtime error fetching combined feed for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Error processing activity feed: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching combined feed for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve combined activities", null));
        }
    }

    /**
     * Combined activity feed grouped by date for timeline display.
     */
    @GetMapping("/activities/combined/grouped")
    public ResponseEntity<ApiResponse<java.util.Map<LocalDate, List<ActivityFeedService.CombinedActivityItem>>>> getCombinedActivityFeedGrouped(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            java.util.Map<LocalDate, List<ActivityFeedService.CombinedActivityItem>> activities =
                activityFeedService.getCombinedActivityFeedGroupedByDate(project.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Grouped activities retrieved successfully", activities));
        } catch (RuntimeException e) {
            logger.error("Runtime error fetching grouped feed for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Error processing grouped activity feed: " + e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching grouped feed for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve grouped activities", null));
        }
    }

    // ===== GALLERY ENDPOINTS =====

    @PostMapping(value = "/gallery", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GalleryImageDto>> uploadImage(
            @PathVariable("projectId") String projectUuid,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) LocalDate takenDate,
            @RequestParam(required = false) Long siteReportId,
            @RequestParam(required = false) String locationTag,
            @RequestParam(required = false) List<String> tags,
            Authentication auth) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Image file is required and cannot be empty", null));
            }
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            Long userId = resolveUserId(email);
            GalleryUploadRequest request = new GalleryUploadRequest(caption, takenDate, siteReportId, locationTag, tags);
            GalleryImageDto image = galleryService.uploadImage(project.getId(), file, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Image uploaded successfully", image));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "upload image", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to upload image for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to upload image", null));
        }
    }

    @GetMapping("/gallery")
    public ResponseEntity<ApiResponse<List<GalleryImageDto>>> getGalleryImages(
            @PathVariable("projectId") String projectUuid,
            @RequestParam(required = false) LocalDate date,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<GalleryImageDto> images;
            if (date != null) {
                images = galleryService.getImagesByDate(project.getId(), date);
            } else {
                images = galleryService.getProjectImages(project.getId());
            }
            return ResponseEntity.ok(new ApiResponse<>(true, "Gallery images retrieved successfully", images));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get gallery images", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get gallery images for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve gallery images", null));
        }
    }

    /**
     * Get gallery images grouped by date for timeline display.
     */
    @GetMapping("/gallery/grouped")
    public ResponseEntity<ApiResponse<java.util.Map<LocalDate, List<GalleryImageDto>>>> getGalleryImagesGrouped(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            java.util.Map<LocalDate, List<GalleryImageDto>> groupedImages = galleryService.getImagesGroupedByDate(project.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Gallery images grouped by date", groupedImages));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get grouped gallery images", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get grouped gallery images for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve grouped gallery images", null));
        }
    }

    // ===== OBSERVATION (SNAGS) ENDPOINTS =====

    @PostMapping(value = "/observations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ObservationDto>> createObservation(
            @PathVariable("projectId") String projectUuid,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) Long reportedByRoleId,
            @RequestParam String priority,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) MultipartFile image,
            Authentication auth) {
        try {
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Title is required", null));
            }
            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Description is required", null));
            }
            if (priority == null || priority.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Priority is required", null));
            }
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            Long userId = resolveUserId(email);
            ObservationRequest request = new ObservationRequest(title, description, reportedByRoleId, priority, location);
            ObservationDto obs = observationService.createObservation(project.getId(), request, image, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Observation created successfully", obs));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "create observation", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to create observation for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to create observation", null));
        }
    }

    @GetMapping("/observations")
    public ResponseEntity<ApiResponse<List<ObservationDto>>> getObservations(
            @PathVariable("projectId") String projectUuid,
            @RequestParam(required = false) String status,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<ObservationDto> observations = observationService.getObservations(project.getId(), status);
            return ResponseEntity.ok(new ApiResponse<>(true, "Observations retrieved successfully", observations));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get observations", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get observations for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve observations", null));
        }
    }

    @GetMapping("/observations/active")
    public ResponseEntity<ApiResponse<List<ObservationDto>>> getActiveObservations(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<ObservationDto> observations = observationService.getActiveObservations(project.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Active observations retrieved", observations));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get active observations", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get active observations for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve active observations", null));
        }
    }

    @GetMapping("/observations/resolved")
    public ResponseEntity<ApiResponse<List<ObservationDto>>> getResolvedObservations(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<ObservationDto> observations = observationService.getResolvedObservations(project.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Resolved observations retrieved", observations));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get resolved observations", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get resolved observations for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve resolved observations", null));
        }
    }

    @GetMapping("/observations/counts")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getObservationCounts(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            java.util.Map<String, Long> counts = observationService.getObservationCounts(project.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Observation counts retrieved", counts));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get observation counts", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get observation counts for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve observation counts", null));
        }
    }

    @PutMapping("/observations/{obsId}")
    public ResponseEntity<ApiResponse<ObservationDto>> resolveObservation(
            @PathVariable("projectId") String projectUuid,
            @PathVariable Long obsId,
            @RequestBody ObservationResolveRequest request,
            Authentication auth) {
        try {
            String email = auth.getName();
            dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            Long userId = resolveUserId(email);
            ObservationDto obs = observationService.resolveObservation(obsId, request, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Observation resolved successfully", obs));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "resolve observation", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to resolve observation {} for project {}: {}", obsId, projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to resolve observation", null));
        }
    }

    // ===== QUERY ENDPOINTS =====

    @PostMapping("/queries")
    public ResponseEntity<ApiResponse<ProjectQueryDto>> createQuery(
            @PathVariable Long projectId,
            @RequestBody ProjectQueryRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            ProjectQueryDto query = queryService.createQuery(projectId, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Query created successfully", query));
        } catch (Exception e) {
            logger.error("Failed to create query for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to create query", null));
        }
    }

    @GetMapping("/queries")
    public ResponseEntity<ApiResponse<List<ProjectQueryDto>>> getQueries(
            @PathVariable Long projectId,
            @RequestParam(required = false) String status) {
        try {
            List<ProjectQueryDto> queries = queryService.getQueries(projectId, status);
            return ResponseEntity.ok(new ApiResponse<>(true, "Queries retrieved successfully", queries));
        } catch (Exception e) {
            logger.error("Failed to get queries for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve queries", null));
        }
    }

    @PutMapping("/queries/{queryId}")
    public ResponseEntity<ApiResponse<ProjectQueryDto>> resolveQuery(
            @PathVariable Long projectId,
            @PathVariable Long queryId,
            @RequestBody ProjectQueryResolveRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            ProjectQueryDto query = queryService.resolveQuery(queryId, request, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Query resolved successfully", query));
        } catch (Exception e) {
            logger.error("Failed to resolve query {} for project {}: {}", queryId, projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to resolve query", null));
        }
    }

    // ===== CCTV ENDPOINTS =====

    @PostMapping("/cctv")
    public ResponseEntity<ApiResponse<CctvCameraDto>> addCamera(
            @PathVariable Long projectId,
            @RequestBody CctvCameraRequest request) {
        try {
            CctvCameraDto camera = cctvService.addCamera(projectId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Camera added successfully", camera));
        } catch (Exception e) {
            logger.error("Failed to add camera for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to add camera", null));
        }
    }

    @GetMapping("/cctv")
    public ResponseEntity<ApiResponse<List<CctvCameraDto>>> getCameras(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "false") boolean installedOnly) {
        try {
            List<CctvCameraDto> cameras;
            if (installedOnly) {
                cameras = cctvService.getInstalledCameras(projectId);
            } else {
                cameras = cctvService.getProjectCameras(projectId);
            }
            return ResponseEntity.ok(new ApiResponse<>(true, "Cameras retrieved successfully", cameras));
        } catch (Exception e) {
            logger.error("Failed to get cameras for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve cameras", null));
        }
    }

    @PutMapping("/cctv/{cameraId}")
    public ResponseEntity<ApiResponse<CctvCameraDto>> updateCamera(
            @PathVariable Long projectId,
            @PathVariable Long cameraId,
            @RequestBody CctvCameraRequest request) {
        try {
            CctvCameraDto camera = cctvService.updateCamera(cameraId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Camera updated successfully", camera));
        } catch (Exception e) {
            logger.error("Failed to update camera {} for project {}: {}", cameraId, projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to update camera", null));
        }
    }

    // ===== 360 VIEW ENDPOINTS =====

    @PostMapping("/360-views")
    public ResponseEntity<ApiResponse<View360Dto>> add360View(
            @PathVariable Long projectId,
            @RequestBody View360Request request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            View360Dto view = view360Service.addView360(projectId, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "360 view added successfully", view));
        } catch (Exception e) {
            logger.error("Failed to add 360 view for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to add 360 view", null));
        }
    }

    @GetMapping("/360-views")
    public ResponseEntity<ApiResponse<List<View360Dto>>> get360Views(
            @PathVariable Long projectId) {
        try {
            List<View360Dto> views = view360Service.getProjectViews(projectId);
            return ResponseEntity.ok(new ApiResponse<>(true, "360 views retrieved successfully", views));
        } catch (Exception e) {
            logger.error("Failed to get 360 views for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve 360 views", null));
        }
    }

    @PostMapping("/360-views/{viewId}/increment-count")
    public ResponseEntity<ApiResponse<View360Dto>> incrementViewCount(
            @PathVariable Long projectId,
            @PathVariable Long viewId) {
        try {
            View360Dto view = view360Service.incrementViewCount(viewId);
            return ResponseEntity.ok(new ApiResponse<>(true, "View count incremented", view));
        } catch (Exception e) {
            logger.error("Failed to increment view count for view {} in project {}: {}", viewId, projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to increment view count", null));
        }
    }

    // ===== SITE VISIT ENDPOINTS =====

    @PostMapping("/site-visits/check-in")
    public ResponseEntity<ApiResponse<SiteVisitDto>> checkIn(
            @PathVariable("projectId") String projectUuid,
            @RequestBody SiteVisitCheckInRequest request,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            Long userId = resolveUserId(email);
            SiteVisitDto visit = siteVisitService.checkIn(project.getId(), request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Checked in successfully", visit));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "check in", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to check in for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to check in", null));
        }
    }

    @PutMapping("/site-visits/{visitId}/check-out")
    public ResponseEntity<ApiResponse<SiteVisitDto>> checkOut(
            @PathVariable("projectId") String projectUuid,
            @PathVariable Long visitId,
            @RequestBody SiteVisitCheckOutRequest request,
            Authentication auth) {
        try {
            String email = auth.getName();
            dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            SiteVisitDto visit = siteVisitService.checkOut(visitId, request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Checked out successfully", visit));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "check out", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to check out for visit {} in project {}: {}", visitId, projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to check out", null));
        }
    }

    @GetMapping("/site-visits")
    public ResponseEntity<ApiResponse<List<SiteVisitDto>>> getSiteVisits(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<SiteVisitDto> visits = siteVisitService.getProjectVisits(project.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Site visits retrieved successfully", visits));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get site visits", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get site visits for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve site visits", null));
        }
    }

    @GetMapping("/site-visits/completed")
    public ResponseEntity<ApiResponse<List<SiteVisitDto>>> getCompletedSiteVisits(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<SiteVisitDto> visits = siteVisitService.getCompletedVisits(project.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Completed site visits retrieved", visits));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get completed visits", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get completed site visits for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve completed site visits", null));
        }
    }

    @GetMapping("/site-visits/ongoing")
    public ResponseEntity<ApiResponse<List<SiteVisitDto>>> getOngoingSiteVisits(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<SiteVisitDto> visits = siteVisitService.getOngoingVisits(project.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Ongoing site visits retrieved", visits));
        } catch (RuntimeException e) {
            return handleRuntimeException(e, "get ongoing visits", projectUuid, auth);
        } catch (Exception e) {
            logger.error("Failed to get ongoing site visits for project {}: {}", projectUuid, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve ongoing site visits", null));
        }
    }

    // ===== FEEDBACK ENDPOINTS =====

    @PostMapping("/feedback/forms")
    public ResponseEntity<ApiResponse<FeedbackFormDto>> createFeedbackForm(
            @PathVariable Long projectId,
            @RequestBody FeedbackFormRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            FeedbackFormDto form = feedbackService.createForm(projectId, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Feedback form created successfully", form));
        } catch (Exception e) {
            logger.error("Failed to create feedback form for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to create feedback form", null));
        }
    }

    @GetMapping("/feedback")
    public ResponseEntity<ApiResponse<List<FeedbackFormDto>>> getFeedbackForms(
            @PathVariable Long projectId,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            List<FeedbackFormDto> forms = feedbackService.getProjectForms(projectId, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Feedback forms retrieved successfully", forms));
        } catch (Exception e) {
            logger.error("Failed to get feedback forms for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve feedback forms", null));
        }
    }

    @PostMapping("/feedback/{formId}/responses")
    public ResponseEntity<ApiResponse<FeedbackResponseDto>> submitFeedback(
            @PathVariable Long projectId,
            @PathVariable Long formId,
            @RequestBody FeedbackResponseRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            FeedbackResponseDto response = feedbackService.submitResponse(formId, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Feedback submitted successfully", response));
        } catch (Exception e) {
            logger.error("Failed to submit feedback for form {} in project {}: {}", formId, projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to submit feedback", null));
        }
    }

    @GetMapping("/feedback/{formId}/responses")
    public ResponseEntity<ApiResponse<List<FeedbackResponseDto>>> getFeedbackResponses(
            @PathVariable Long projectId,
            @PathVariable Long formId) {
        try {
            List<FeedbackResponseDto> responses = feedbackService.getFormResponses(formId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Responses retrieved successfully", responses));
        } catch (Exception e) {
            logger.error("Failed to get feedback responses for form {} in project {}: {}", formId, projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve feedback responses", null));
        }
    }

    // ===== BOQ ENDPOINTS =====

    @PostMapping("/boq")
    public ResponseEntity<ApiResponse<BoqItemDto>> addBoqItem(
            @PathVariable Long projectId,
            @RequestBody BoqItemRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            BoqItemDto item = boqService.addBoqItem(projectId, request, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "BoQ item added successfully", item));
        } catch (Exception e) {
            logger.error("Failed to add BOQ item for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to add BoQ item", null));
        }
    }

    @PutMapping("/boq/{itemId}")
    public ResponseEntity<ApiResponse<BoqItemDto>> updateBoqItem(
            @PathVariable Long projectId,
            @PathVariable Long itemId,
            @RequestBody BoqItemRequest request,
            Authentication auth) {
        try {
            Long userId = getUserIdFromAuth(auth);
            BoqItemDto item = boqService.updateBoqItem(itemId, request, userId);
            return ResponseEntity.ok(new ApiResponse<>(true, "BoQ item updated successfully", item));
        } catch (Exception e) {
            logger.error("Failed to update BOQ item {} for project {}: {}", itemId, projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to update BoQ item", null));
        }
    }

    @GetMapping("/boq")
    public ResponseEntity<ApiResponse<List<BoqItemDto>>> getBoqItems(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long workTypeId) {
        try {
            List<BoqItemDto> items;
            if (workTypeId != null) {
                items = boqService.getBoqItemsByWorkType(projectId, workTypeId);
            } else {
                items = boqService.getProjectBoqItems(projectId);
            }
            return ResponseEntity.ok(new ApiResponse<>(true, "BoQ items retrieved successfully", items));
        } catch (Exception e) {
            logger.error("Failed to get BOQ items for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve BoQ items", null));
        }
    }

    @GetMapping("/boq/summary")
    public ResponseEntity<ApiResponse<BoqSummaryDto>> getBoqSummary(
            @PathVariable Long projectId) {
        try {
            BoqSummaryDto summary = boqService.getBoqSummary(projectId);
            return ResponseEntity.ok(new ApiResponse<>(true, "BoQ summary retrieved successfully", summary));
        } catch (Exception e) {
            logger.error("Failed to get BOQ summary for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve BoQ summary", null));
        }
    }

    @GetMapping("/boq/work-types")
    public ResponseEntity<ApiResponse<List<BoqWorkTypeDto>>> getWorkTypes() {
        try {
            List<BoqWorkTypeDto> workTypes = boqService.getAllWorkTypes();
            return ResponseEntity.ok(new ApiResponse<>(true, "Work types retrieved successfully", workTypes));
        } catch (Exception e) {
            logger.error("Failed to get work types: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Failed to retrieve work types", null));
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Extract user ID from authentication.
     * Falls back to resolving by email if name is not numeric.
     */
    private Long getUserIdFromAuth(Authentication auth) {
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            // Name is an email, resolve from repository
            return resolveUserId(auth.getName());
        }
    }

    /**
     * Resolve user ID from email address.
     * @throws RuntimeException if user not found
     */
    private Long resolveUserId(String email) {
        return customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email))
                .getId();
    }

    /**
     * Centralized RuntimeException handler for common patterns.
     * Handles "User not found", "Project not found", access denied, etc.
     */
    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ApiResponse<T>> handleRuntimeException(
            RuntimeException e, String operation, String projectUuid, Authentication auth) {
        String message = e.getMessage();
        if (message != null && message.toLowerCase().contains("user not found")) {
            logger.error("User not found during {} for project {}, user {}: {}",
                operation, projectUuid, auth != null ? auth.getName() : "unknown", message);
            return (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "User account not found", null));
        }
        if (message != null && message.toLowerCase().contains("not found")) {
            logger.warn("Resource not found during {} for project {}: {}", operation, projectUuid, message);
            return (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, message, null));
        }
        if (message != null && (message.toLowerCase().contains("access denied") 
                || message.toLowerCase().contains("not authorized"))) {
            logger.warn("Access denied during {} for project {} by user {}: {}",
                operation, projectUuid, auth != null ? auth.getName() : "unknown", message);
            return (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(false, "Access denied", null));
        }
        logger.error("Runtime error during {} for project {}: {}", operation, projectUuid, message, e);
        return (ResponseEntity<ApiResponse<T>>) (ResponseEntity<?>) ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse<>(false, "Operation failed: " + operation, null));
    }
}
