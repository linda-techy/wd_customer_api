package com.wd.custapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Centralized DTOs for all project modules
 */
public class ProjectModuleDtos {

    // ===== PROJECT PHASE MODULE DTOs =====

    /**
     * Customer-facing view of a construction phase.
     * Status values: NOT_STARTED | IN_PROGRESS | COMPLETED | DELAYED
     */
    public record ProjectPhaseDto(
        Long id,
        String phaseName,
        String status,
        Integer displayOrder,
        LocalDate plannedStart,
        LocalDate plannedEnd,
        LocalDate actualStart,
        LocalDate actualEnd
    ) {}

    // ===== DOCUMENT MODULE DTOs =====
    
    public record DocumentCategoryDto(
        Long id,
        String name,
        String description,
        Integer displayOrder
    ) {}
    
    public record ProjectDocumentDto(
        Long id,
        Long projectId,
        Long categoryId,
        String categoryName,
        String filename,
        String filePath,
        String downloadUrl,  // Full URL for downloading/viewing the file
        Long fileSize,
        String fileType,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime uploadDate,
        String description,
        Integer version,
        Boolean isActive
    ) {}
    
    public record DocumentUploadRequest(
        Long categoryId,
        String description
    ) {}
    
    // ===== QUALITY CHECK MODULE DTOs =====
    
    public record QualityCheckDto(
        Long id,
        Long projectId,
        String title,
        String description,
        String sopReference,
        String status,
        String priority,
        @JsonIgnore Long assignedToId,
        @JsonIgnore String assignedToName,
        @JsonIgnore Long createdById,
        @JsonIgnore String createdByName,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        @JsonIgnore Long resolvedById,
        @JsonIgnore String resolvedByName,
        String resolutionNotes
    ) {}
    
    public record QualityCheckRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Description is required") String description,
        String sopReference,
        String priority,
        Long assignedToId
    ) {}
    
    public record QualityCheckUpdateRequest(
        String resolutionNotes
    ) {}
    
    // ===== ACTIVITY FEED MODULE DTOs =====
    
    public record ActivityFeedDto(
        Long id,
        Long projectId,
        String activityTypeName,
        String activityTypeIcon,
        String activityTypeColor,
        String title,
        String description,
        Long referenceId,
        String referenceType,
        @JsonIgnore Long createdById,
        @JsonIgnore String createdByName,
        LocalDateTime createdAt,
        Map<String, Object> metadata
    ) {}
    
    public record ActivityFeedRequest(
        String activityTypeName,
        String title,
        String description,
        Long referenceId,
        String referenceType,
        Map<String, Object> metadata
    ) {}
    
    // ===== GALLERY MODULE DTOs =====
    
    public record GalleryImageDto(
        Long id,
        Long projectId,
        String imagePath,
        String thumbnailPath,
        String caption,
        LocalDate takenDate,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime uploadedAt,
        Long siteReportId,
        String locationTag,
        List<String> tags
    ) {}
    
    public record GalleryUploadRequest(
        String caption,
        LocalDate takenDate,
        Long siteReportId,
        String locationTag,
        List<String> tags
    ) {}
    
    // ===== OBSERVATION MODULE DTOs =====
    
    public record ObservationDto(
        Long id,
        Long projectId,
        String title,
        String description,
        @JsonIgnore Long reportedById,
        @JsonIgnore String reportedByName,
        Long reportedByRoleId,
        String reportedByRoleName,
        LocalDateTime reportedDate,
        String status,
        String priority,
        String location,
        String imagePath,
        LocalDateTime resolvedDate,
        @JsonIgnore Long resolvedById,
        @JsonIgnore String resolvedByName,
        String resolutionNotes
    ) {}
    
    public record ObservationRequest(
        String title,
        String description,
        Long reportedByRoleId,
        String priority,
        String location
    ) {}
    
    public record ObservationResolveRequest(
        String resolutionNotes
    ) {}
    
    // ===== QUERY MODULE DTOs =====
    
    public record ProjectQueryDto(
        Long id,
        Long projectId,
        String title,
        String description,
        @JsonIgnore Long raisedById,
        @JsonIgnore String raisedByName,
        Long raisedByRoleId,
        String raisedByRoleName,
        LocalDateTime raisedDate,
        String status,
        String priority,
        String category,
        @JsonIgnore Long assignedToId,
        @JsonIgnore String assignedToName,
        LocalDateTime resolvedDate,
        @JsonIgnore Long resolvedById,
        @JsonIgnore String resolvedByName,
        String resolution
    ) {}
    
    public record ProjectQueryRequest(
        @NotBlank(message = "Title is required") String title,
        @NotBlank(message = "Description is required") String description,
        Long raisedByRoleId,
        @NotBlank(message = "Priority is required") String priority,
        String category,
        Long assignedToId
    ) {}
    
    public record ProjectQueryResolveRequest(
        String resolution
    ) {}
    
    // ===== CCTV MODULE DTOs =====

    public record CctvCameraDto(
        Long id,
        Long projectId,
        String cameraName,
        String location,
        String provider,
        String streamProtocol,
        String streamUrl,
        String snapshotUrl,
        Boolean isActive,
        LocalDate installationDate,
        String resolution,
        Integer displayOrder
    ) {}
    
    // ===== 360 VIEW MODULE DTOs =====
    
    public record View360Dto(
        Long id,
        Long projectId,
        String title,
        String description,
        String viewUrl,
        String thumbnailUrl,
        LocalDate captureDate,
        String location,
        Long uploadedById,
        String uploadedByName,
        LocalDateTime uploadedAt,
        Boolean isActive,
        Integer viewCount
    ) {}
    
    public record View360Request(
        String title,
        String description,
        String viewUrl,
        String thumbnailUrl,
        LocalDate captureDate,
        String location
    ) {}
    
    // ===== SITE VISIT MODULE DTOs =====
    
    public record SiteVisitDto(
        Long id,
        Long projectId,
        // visitorId/visitorName ARE exposed to the customer Flutter — the
        // SiteVisits screen renders the visitor's initial as the avatar
        // and their name on each card. The @JsonIgnore that used to hide
        // these caused "type 'Null' is not a subtype of type 'int'"
        // Flutter crashes because the Dart model declares both as
        // non-nullable.
        Long visitorId,
        String visitorName,
        Long visitorRoleId,
        String visitorRoleName,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        String purpose,
        String notes,
        String findings,
        String location,
        String weatherConditions,
        List<String> attendees,
        @JsonIgnore Double checkInLatitude,
        @JsonIgnore Double checkInLongitude,
        @JsonIgnore Double checkOutLatitude,
        @JsonIgnore Double checkOutLongitude,
        @JsonIgnore Double distanceFromProjectCheckIn,
        @JsonIgnore Double distanceFromProjectCheckOut
    ) {}
    
    public record SiteVisitCheckInRequest(
        Long visitorRoleId,
        String purpose,
        String location,
        String weatherConditions,
        List<String> attendees,
        Double latitude,
        Double longitude
    ) {}
    
    public record SiteVisitCheckOutRequest(
        String notes,
        String findings,
        Double latitude,
        Double longitude
    ) {}
    
    // ===== FEEDBACK MODULE DTOs =====
    
    public record FeedbackFormDto(
        Long id,
        Long projectId,
        String title,
        String description,
        String formType,
        @JsonIgnore Long createdById,
        @JsonIgnore String createdByName,
        LocalDateTime createdAt,
        Boolean isActive,
        Boolean isCompleted
    ) {}
    
    public record FeedbackFormRequest(
        String title,
        String description,
        String formType
    ) {}
    
    public record FeedbackResponseDto(
        Long id,
        Long formId,
        String formTitle,
        Long projectId,
        Long customerId,
        String customerName,
        Integer rating,
        String comments,
        Map<String, Object> responseData,
        LocalDateTime submittedAt,
        Boolean isCompleted
    ) {}
    
    public record FeedbackResponseRequest(
        Integer rating,
        String comments,
        Map<String, Object> responseData
    ) {}
    
    // ===== BOQ MODULE DTOs =====
    
    public record BoqWorkTypeDto(
        Long id,
        String name,
        String description,
        Integer displayOrder
    ) {}
    
    public record BoqItemDto(
        Long id,
        Long projectId,
        Long workTypeId,
        String workTypeName,
        Long categoryId,
        String categoryName,
        String itemCode,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal rate,
        BigDecimal amount,
        // Financial tracking fields (READ-ONLY for customers)
        BigDecimal executedQuantity,
        BigDecimal billedQuantity,
        BigDecimal remainingQuantity,
        BigDecimal totalExecutedAmount,
        BigDecimal totalBilledAmount,
        BigDecimal executionPercentage,
        BigDecimal billingPercentage,
        String status,
        String specifications,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        @JsonIgnore Long createdById,
        @JsonIgnore String createdByName,
        Boolean isActive,
        // Scope classification: BASE | ADDON | OPTIONAL | EXCLUSION
        String itemKind
    ) {}
    
    public record BoqItemRequest(
        Long workTypeId,
        String itemCode,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal rate,
        String specifications,
        String notes
    ) {}
    
    public record BoqSummaryDto(
        Long projectId,
        BigDecimal totalPlannedAmount,
        BigDecimal totalExecutedAmount,
        BigDecimal totalBilledAmount,
        BigDecimal executionPercentage,
        BigDecimal billingPercentage,
        int totalItems,
        List<BoqWorkTypeSummary> workTypeSummaries,
        // Add-on breakdown — null when no add-on items exist
        BigDecimal baseScopeAmount,
        BigDecimal addonAmount
    ) {}
    
    public record BoqWorkTypeSummary(
        Long workTypeId,
        String workTypeName,
        BigDecimal subtotal,
        Integer itemCount
    ) {}
    
    public record BoqApprovalRequest(
        @NotBlank(message = "status is required") String status,
        @Size(max = 2000, message = "message must not exceed 2000 characters") String message
    ) {}

    // ===== SITE REPORT MODULE DTOs =====
    
    public record SiteReportDto(
        Long id,
        Long projectId,
        String projectName,
        LocalDateTime reportDate,
        String title,
        String description,
        String status,
        String reportType,
        @JsonIgnore Long submittedById,
        @JsonIgnore String submittedByName,
        LocalDateTime createdAt
    ) {}
    
    public record SiteReportRequest(
        LocalDateTime reportDate,
        String title,
        String description,
        String reportType
    ) {}
    
    // ===== COMMON DTOs =====
    
    public record StaffRoleDto(
        Long id,
        String name
    ) {}
    
    public record ApiResponse<T>(
        boolean success,
        String message,
        T data
    ) {}
    
    public record PaginatedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {}
    
    public record FileUploadResponse(
        String filename,
        String filePath,
        Long fileSize,
        String message
    ) {}
}

