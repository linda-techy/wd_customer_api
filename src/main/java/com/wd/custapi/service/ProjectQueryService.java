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
public class ProjectQueryService {
    
    private final ProjectQueryRepository queryRepository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final ActivityFeedService activityFeedService;
    
    public ProjectQueryService(ProjectQueryRepository queryRepository,
                               ProjectRepository projectRepository,
                               CustomerUserRepository userRepository,
                               StaffRoleRepository staffRoleRepository,
                               ActivityFeedService activityFeedService) {
        this.queryRepository = queryRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.activityFeedService = activityFeedService;
    }
    
    @Transactional
    public ProjectQueryDto createQuery(Long projectId, ProjectQueryRequest request, Long userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        CustomerUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        ProjectQuery query = new ProjectQuery();
        query.setProject(project);
        query.setTitle(request.title());
        query.setDescription(request.description());
        query.setRaisedBy(user);
        query.setPriority(ProjectQuery.Priority.valueOf(request.priority()));
        query.setCategory(request.category());
        
        if (request.raisedByRoleId() != null) {
            StaffRole role = staffRoleRepository.findById(request.raisedByRoleId())
                .orElseThrow(() -> new RuntimeException("Staff role not found"));
            query.setRaisedByRole(role);
        }
        
        if (request.assignedToId() != null) {
            CustomerUser assignedTo = userRepository.findById(request.assignedToId())
                .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            query.setAssignedTo(assignedTo);
        }
        
        query = queryRepository.save(query);
        
        // Create activity feed
        activityFeedService.createActivity(projectId, "QUERY_ADDED", 
            "Query added: " + request.title(), query.getId(), userId);
        
        return toDto(query);
    }
    
    @Transactional
    public ProjectQueryDto resolveQuery(Long queryId, ProjectQueryResolveRequest request, Long userId) {
        ProjectQuery query = queryRepository.findById(queryId)
            .orElseThrow(() -> new RuntimeException("Query not found"));
        
        CustomerUser resolvedBy = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        query.setStatus(ProjectQuery.QueryStatus.RESOLVED);
        query.setResolvedDate(LocalDateTime.now());
        query.setResolvedBy(resolvedBy);
        query.setResolution(request.resolution());
        
        query = queryRepository.save(query);
        
        // Create activity feed
        activityFeedService.createActivity(query.getProject().getId(), "QUERY_RESOLVED", 
            "Query resolved: " + query.getTitle(), query.getId(), userId);
        
        return toDto(query);
    }
    
    public List<ProjectQueryDto> getQueries(Long projectId, String status) {
        List<ProjectQuery> queries;
        if (status != null) {
            ProjectQuery.QueryStatus queryStatus = ProjectQuery.QueryStatus.valueOf(status);
            queries = queryRepository.findByProjectIdAndStatusOrderByPriorityDescRaisedDateDesc(projectId, queryStatus);
        } else {
            queries = queryRepository.findByProjectIdOrderByRaisedDateDesc(projectId);
        }
        return queries.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    private ProjectQueryDto toDto(ProjectQuery query) {
        return new ProjectQueryDto(
            query.getId(),
            query.getProject().getId(),
            query.getTitle(),
            query.getDescription(),
            query.getRaisedBy().getId(),
            query.getRaisedBy().getFirstName() + " " + query.getRaisedBy().getLastName(),
            query.getRaisedByRole() != null ? query.getRaisedByRole().getId() : null,
            query.getRaisedByRole() != null ? query.getRaisedByRole().getName() : null,
            query.getRaisedDate(),
            query.getStatus().name(),
            query.getPriority().name(),
            query.getCategory(),
            query.getAssignedTo() != null ? query.getAssignedTo().getId() : null,
            query.getAssignedTo() != null ? query.getAssignedTo().getFirstName() + " " + query.getAssignedTo().getLastName() : null,
            query.getResolvedDate(),
            query.getResolvedBy() != null ? query.getResolvedBy().getId() : null,
            query.getResolvedBy() != null ? query.getResolvedBy().getFirstName() + " " + query.getResolvedBy().getLastName() : null,
            query.getResolution()
        );
    }
}

