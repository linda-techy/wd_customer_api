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
    
    public ActivityFeedService(ActivityFeedRepository activityFeedRepository,
                               ActivityTypeRepository activityTypeRepository,
                               ProjectRepository projectRepository,
                               CustomerUserRepository userRepository,
                               SiteReportRepository siteReportRepository,
                               ProjectQueryRepository projectQueryRepository) {
        this.activityFeedRepository = activityFeedRepository;
        this.activityTypeRepository = activityTypeRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.siteReportRepository = siteReportRepository;
        this.projectQueryRepository = projectQueryRepository;
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
        return new ActivityFeedDto(
            activity.getId(),
            activity.getProject().getId(),
            activity.getActivityType().getName(),
            activity.getActivityType().getIcon(),
            activity.getActivityType().getColor(),
            activity.getTitle(),
            activity.getDescription(),
            activity.getReferenceId(),
            activity.getReferenceType(),
            activity.getCreatedBy().getId(),
            activity.getCreatedBy().getFirstName() + " " + activity.getCreatedBy().getLastName(),
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
     * Get combined activity feed with site reports and queries.
     * Returns items sorted by date descending.
     */
    public List<CombinedActivityItem> getCombinedActivityFeed(Long projectId) {
        List<SiteReport> siteReports = siteReportRepository.findByProjectIdOrderByReportDateDesc(projectId);
        List<ProjectQuery> queries = projectQueryRepository.findByProjectIdOrderByRaisedDateDesc(projectId);
        
        List<CombinedActivityItem> siteReportItems = siteReports.stream()
            .map(this::toActivityItem)
            .collect(Collectors.toList());
        
        List<CombinedActivityItem> queryItems = queries.stream()
            .map(this::toActivityItem)
            .collect(Collectors.toList());
        
        // Merge and sort by timestamp descending
        return Stream.concat(siteReportItems.stream(), queryItems.stream())
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
        metadata.put("weather", report.getWeather());
        metadata.put("workProgress", report.getWorkProgress());
        metadata.put("manpowerDeployed", report.getManpowerDeployed());
        
        String createdByName = "Company";
        if (report.getCreatedBy() != null) {
            createdByName = report.getCreatedBy().getFirstName() + " " + report.getCreatedBy().getLastName();
        } else if (report.getSubmittedByName() != null) {
            createdByName = report.getSubmittedByName();
        }
        
        LocalDateTime timestamp = report.getReportDate() != null 
            ? report.getReportDate().atStartOfDay() 
            : report.getCreatedAt();
        
        return new CombinedActivityItem(
            report.getId(),
            "SITE_REPORT",
            report.getTitle(),
            report.getDescription(),
            timestamp,
            report.getReportDate(),
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
}

