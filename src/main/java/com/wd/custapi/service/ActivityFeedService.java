package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActivityFeedService {
    
    private final ActivityFeedRepository activityFeedRepository;
    private final ActivityTypeRepository activityTypeRepository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    
    public ActivityFeedService(ActivityFeedRepository activityFeedRepository,
                               ActivityTypeRepository activityTypeRepository,
                               ProjectRepository projectRepository,
                               CustomerUserRepository userRepository) {
        this.activityFeedRepository = activityFeedRepository;
        this.activityTypeRepository = activityTypeRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
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
}

