package com.wd.custapi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Centralized DTOs for all project modules
 */
public class ProjectModuleDtos {
    
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
        Long assignedToId,
        String assignedToName,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        Long resolvedById,
        String resolvedByName,
        String resolutionNotes
    ) {}
    
    public record QualityCheckRequest(
        String title,
        String description,
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
        Long createdById,
        String createdByName,
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
        Long reportedById,
        String reportedByName,
        Long reportedByRoleId,
        String reportedByRoleName,
        LocalDateTime reportedDate,
        String status,
        String priority,
        String location,
        String imagePath,
        LocalDateTime resolvedDate,
        Long resolvedById,
        String resolvedByName,
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
        Long raisedById,
        String raisedByName,
        Long raisedByRoleId,
        String raisedByRoleName,
        LocalDateTime raisedDate,
        String status,
        String priority,
        String category,
        Long assignedToId,
        String assignedToName,
        LocalDateTime resolvedDate,
        Long resolvedById,
        String resolvedByName,
        String resolution
    ) {}
    
    public record ProjectQueryRequest(
        String title,
        String description,
        Long raisedByRoleId,
        String priority,
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
        String streamUrl,
        String snapshotUrl,
        Boolean isInstalled,
        Boolean isActive,
        LocalDate installationDate,
        LocalDateTime lastActive,
        String cameraType,
        String resolution,
        String notes
    ) {}
    
    public record CctvCameraRequest(
        String cameraName,
        String location,
        String streamUrl,
        String snapshotUrl,
        Boolean isInstalled,
        String cameraType,
        String resolution,
        String notes
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
        Double checkInLatitude,
        Double checkInLongitude,
        Double checkOutLatitude,
        Double checkOutLongitude,
        Double distanceFromProjectCheckIn,
        Double distanceFromProjectCheckOut
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
        Long createdById,
        String createdByName,
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
        String itemCode,
        String description,
        BigDecimal quantity,
        String unit,
        BigDecimal rate,
        BigDecimal amount,
        String specifications,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long createdById,
        String createdByName,
        Boolean isActive
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
        BigDecimal totalAmount,
        List<BoqWorkTypeSummary> workTypeSummaries
    ) {}
    
    public record BoqWorkTypeSummary(
        Long workTypeId,
        String workTypeName,
        BigDecimal subtotal,
        Integer itemCount
    ) {}
    
    // ===== SITE REPORT MODULE DTOs =====
    
    public record SiteReportDto(
        Long id,
        Long projectId,
        LocalDate reportDate,
        String title,
        String description,
        String weather,
        String workProgress,
        Integer manpowerDeployed,
        String equipmentUsed,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt
    ) {}
    
    public record SiteReportRequest(
        LocalDate reportDate,
        String title,
        String description,
        String weather,
        String workProgress,
        Integer manpowerDeployed,
        String equipmentUsed
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

