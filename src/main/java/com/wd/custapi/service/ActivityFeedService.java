package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ActivityFeedService {
    
    private final ActivityFeedRepository activityFeedRepository;
    private final ActivityTypeRepository activityTypeRepository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    private final SiteReportRepository siteReportRepository;
    private final ProjectQueryRepository projectQueryRepository;
    private final ObservationRepository observationRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final GalleryImageRepository galleryImageRepository;
    private final SiteVisitRepository siteVisitRepository;
    
    public ActivityFeedService(ActivityFeedRepository activityFeedRepository,
                               ActivityTypeRepository activityTypeRepository,
                               ProjectRepository projectRepository,
                               CustomerUserRepository userRepository,
                               SiteReportRepository siteReportRepository,
                               ProjectQueryRepository projectQueryRepository,
                               ObservationRepository observationRepository,
                               QualityCheckRepository qualityCheckRepository,
                               GalleryImageRepository galleryImageRepository,
                               SiteVisitRepository siteVisitRepository) {
        this.activityFeedRepository = activityFeedRepository;
        this.activityTypeRepository = activityTypeRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.siteReportRepository = siteReportRepository;
        this.projectQueryRepository = projectQueryRepository;
        this.observationRepository = observationRepository;
        this.qualityCheckRepository = qualityCheckRepository;
        this.galleryImageRepository = galleryImageRepository;
        this.siteVisitRepository = siteVisitRepository;
    }
    
    @Transactional
    public ActivityFeedDto createActivity(Long projectId, String activityTypeName, 
                                          String title, Long referenceId, Long userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        ActivityType activityType = activityTypeRepository.findByName(activityTypeName)
            .orElseThrow(() -> new RuntimeException("Activity type not found"));
        
        CustomerUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        ActivityFeed activity = new ActivityFeed();
        activity.setProject(project);
        activity.setActivityType(activityType);
        activity.setTitle(title);
        activity.setReferenceId(referenceId);
        activity.setCreatedBy(user);
        
        activity = activityFeedRepository.save(activity);
        return toDto(activity);
    }
    
    public List<ActivityFeedDto> getProjectActivities(Long projectId) {
        return activityFeedRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    public List<ActivityFeedDto> getProjectActivitiesByDateRange(Long projectId, 
                                                                 LocalDateTime startDate, 
                                                                 LocalDateTime endDate) {
        return activityFeedRepository.findByProjectIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            projectId, startDate, endDate)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    private ActivityFeedDto toDto(ActivityFeed activity) {
        Long createdById = null;
        String createdByFullName = "Unknown";
        if (activity.getCreatedBy() != null) {
            createdById = activity.getCreatedBy().getId();
            createdByFullName = activity.getCreatedBy().getFirstName() + " " + activity.getCreatedBy().getLastName();
        }
        
        return new ActivityFeedDto(
            activity.getId(),
            activity.getProject() != null ? activity.getProject().getId() : null,
            activity.getActivityType() != null ? activity.getActivityType().getName() : null,
            activity.getActivityType() != null ? activity.getActivityType().getIcon() : null,
            activity.getActivityType() != null ? activity.getActivityType().getColor() : null,
            activity.getTitle(),
            activity.getDescription(),
            activity.getReferenceId(),
            activity.getReferenceType(),
            createdById,
            createdByFullName,
            activity.getCreatedAt(),
            activity.getMetadata()
        );
    }
    
    /**
     * Combined activity item for timeline display.
     * Merges site reports and queries into a single chronological feed.
     */
    public record CombinedActivityItem(
        Long id,
        String type, // "SITE_REPORT" or "QUERY"
        String title,
        String description,
        LocalDateTime timestamp,
        LocalDate date,
        String status,
        String createdByName,
        Map<String, Object> metadata
    ) {}
    
    /**
     * Get combined activity feed with site reports, queries, observations, etc.
     * Returns items sorted by date descending.
     */
    public List<CombinedActivityItem> getCombinedActivityFeed(Long projectId) {
        List<SiteReport> siteReports = siteReportRepository.findByProjectIdOrderByReportDateDesc(projectId);
        List<ProjectQuery> queries = projectQueryRepository.findByProjectIdOrderByRaisedDateDesc(projectId);
        List<Observation> observations = observationRepository.findByProjectIdOrderByReportedDateDesc(projectId);
        List<QualityCheck> checks = qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        List<GalleryImage> images = galleryImageRepository.findByProjectIdOrderByTakenDateDesc(projectId);
        List<SiteVisit> visits = siteVisitRepository.findByProjectIdOrderByCheckInTimeDesc(projectId);
        
        List<CombinedActivityItem> siteReportItems = siteReports.stream()
            .filter(r -> r.getStatus() == null || !"DRAFT".equalsIgnoreCase(r.getStatus()))
            .map(this::toActivityItem)
            .collect(Collectors.toList());
        
        List<CombinedActivityItem> queryItems = queries.stream()
            .map(this::toActivityItem)
            .collect(Collectors.toList());
            
        List<CombinedActivityItem> obsItems = observations.stream()
            .map(this::toActivityItem)
            .collect(Collectors.toList());
            
        List<CombinedActivityItem> qcItems = checks.stream()
            .map(this::toActivityItem)
            .collect(Collectors.toList());
            
        List<CombinedActivityItem> galleryItems = images.stream()
            .map(this::toActivityItem)
            .collect(Collectors.toList());
            
        List<CombinedActivityItem> visitItems = visits.stream()
            .map(this::toActivityItem)
            .collect(Collectors.toList());
        
        // Merge and sort by timestamp descending
        return Stream.of(siteReportItems, queryItems, obsItems, qcItems, galleryItems, visitItems)
            .flatMap(List::stream)
            .sorted(Comparator.comparing(CombinedActivityItem::timestamp).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Get combined activity feed grouped by date.
     */
    public Map<LocalDate, List<CombinedActivityItem>> getCombinedActivityFeedGroupedByDate(Long projectId) {
        List<CombinedActivityItem> activities = getCombinedActivityFeed(projectId);
        return activities.stream()
            .collect(Collectors.groupingBy(
                CombinedActivityItem::date,
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }
    
    /**
     * Get combined activity feed filtered by type.
     */
    public List<CombinedActivityItem> getCombinedActivityFeedByType(Long projectId, String type) {
        List<CombinedActivityItem> all = getCombinedActivityFeed(projectId);
        if (type == null || type.isEmpty() || type.equalsIgnoreCase("ALL")) {
            return all;
        }
        return all.stream()
            .filter(item -> item.type().equalsIgnoreCase(type))
            .collect(Collectors.toList());
    }
    
    private CombinedActivityItem toActivityItem(SiteReport report) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reportType", report.getReportType());
        metadata.put("status", report.getStatus());
        
        String createdByName = "Company Staff";
        if (report.getSubmittedByName() != null && !report.getSubmittedByName().isEmpty()) {
            createdByName = report.getSubmittedByName();
        }
        
        LocalDateTime timestamp = report.getReportDate() != null 
            ? report.getReportDate()
            : report.getCreatedAt();
        
        LocalDate date = report.getReportDate() != null 
            ? report.getReportDate().toLocalDate() 
            : LocalDate.now();
        
        return new CombinedActivityItem(
            report.getId(),
            "SITE_REPORT",
            report.getTitle(),
            report.getDescription(),
            timestamp,
            date,
            report.getStatus(),
            createdByName,
            metadata
        );
    }
    
    private CombinedActivityItem toActivityItem(ProjectQuery query) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", query.getCategory());
        metadata.put("priority", query.getPriority() != null ? query.getPriority().name() : null);
        metadata.put("resolution", query.getResolution());
        
        String createdByName = "Customer";
        if (query.getRaisedBy() != null) {
            createdByName = query.getRaisedBy().getFirstName() + " " + query.getRaisedBy().getLastName();
        }
        
        return new CombinedActivityItem(
            query.getId(),
            "QUERY",
            query.getTitle(),
            query.getDescription(),
            query.getRaisedDate(),
            query.getRaisedDate() != null ? query.getRaisedDate().toLocalDate() : LocalDate.now(),
            query.getStatus() != null ? query.getStatus().name() : null,
            createdByName,
            metadata
        );
    }
    
    private CombinedActivityItem toActivityItem(Observation obs) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("priority", obs.getPriority() != null ? obs.getPriority().name() : null);
        metadata.put("location", obs.getLocation());
        
        String createdByName = "Staff";
        if (obs.getReportedBy() != null) {
            createdByName = obs.getReportedBy().getFirstName() + " " + obs.getReportedBy().getLastName();
        }
        
        return new CombinedActivityItem(
            obs.getId(),
            "OBSERVATION",
            obs.getTitle(),
            obs.getDescription(),
            obs.getReportedDate(),
            obs.getReportedDate() != null ? obs.getReportedDate().toLocalDate() : LocalDate.now(),
            obs.getStatus() != null ? obs.getStatus().name() : null,
            createdByName,
            metadata
        );
    }
    
    private CombinedActivityItem toActivityItem(QualityCheck qc) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("priority", qc.getPriority() != null ? qc.getPriority().name() : null);
        metadata.put("sopReference", qc.getSopReference());
        
        String createdByName = "Staff";
        if (qc.getCreatedBy() != null) {
            createdByName = qc.getCreatedBy().getFirstName() + " " + qc.getCreatedBy().getLastName();
        }
        
        return new CombinedActivityItem(
            qc.getId(),
            "QUALITY_CHECK",
            qc.getTitle(),
            qc.getDescription(),
            qc.getCreatedAt(),
            qc.getCreatedAt() != null ? qc.getCreatedAt().toLocalDate() : LocalDate.now(),
            qc.getStatus() != null ? qc.getStatus().name() : null,
            createdByName,
            metadata
        );
    }
    
    private CombinedActivityItem toActivityItem(GalleryImage img) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("imagePath", img.getImagePath());
        metadata.put("locationTag", img.getLocationTag());
        
        String createdByName = "Staff";
        if (img.getUploadedBy() != null) {
            createdByName = img.getUploadedBy().getFirstName() + " " + img.getUploadedBy().getLastName();
        }
        
        return new CombinedActivityItem(
            img.getId(),
            "GALLERY",
            img.getCaption() != null && !img.getCaption().isEmpty() ? img.getCaption() : "New Photo Uploaded",
            null,
            img.getUploadedAt(),
            img.getTakenDate() != null ? img.getTakenDate() : img.getUploadedAt().toLocalDate(),
            "PUBLISHED",
            createdByName,
            metadata
        );
    }
    
    private CombinedActivityItem toActivityItem(SiteVisit visit) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("purpose", visit.getPurpose());
        metadata.put("location", visit.getLocation());
        
        String createdByName = "Staff";
        if (visit.getVisitor() != null) {
            createdByName = visit.getVisitor().getFirstName() + " " + visit.getVisitor().getLastName();
        }
        
        return new CombinedActivityItem(
            visit.getId(),
            "SITE_VISIT",
            "Site Visit",
            visit.getNotes() != null ? visit.getNotes() : visit.getPurpose(),
            visit.getCheckInTime(),
            visit.getCheckInTime() != null ? visit.getCheckInTime().toLocalDate() : LocalDate.now(),
            visit.getCheckOutTime() != null ? "COMPLETED" : "ONGOING",
            createdByName,
            metadata
        );
    }
}

