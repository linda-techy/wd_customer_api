package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class View360Service {
    
    private final View360Repository view360Repository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    
    public View360Service(View360Repository view360Repository,
                          ProjectRepository projectRepository,
                          CustomerUserRepository userRepository) {
        this.view360Repository = view360Repository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional
    public View360Dto addView360(Long projectId, View360Request request, Long userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        CustomerUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        View360 view = new View360();
        view.setProject(project);
        view.setTitle(request.title());
        view.setDescription(request.description());
        view.setViewUrl(request.viewUrl());
        view.setThumbnailUrl(request.thumbnailUrl());
        view.setCaptureDate(request.captureDate());
        view.setLocation(request.location());
        view.setUploadedBy(user);
        
        view = view360Repository.save(view);
        return toDto(view);
    }
    
    @Transactional
    public View360Dto incrementViewCount(Long viewId) {
        View360 view = view360Repository.findById(viewId)
            .orElseThrow(() -> new RuntimeException("360 view not found"));
        
        view.setViewCount(view.getViewCount() + 1);
        view = view360Repository.save(view);
        return toDto(view);
    }
    
    public List<View360Dto> getProjectViews(Long projectId) {
        return view360Repository.findByProjectIdAndIsActiveTrue(projectId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    private View360Dto toDto(View360 view) {
        return new View360Dto(
            view.getId(),
            view.getProject().getId(),
            view.getTitle(),
            view.getDescription(),
            view.getViewUrl(),
            view.getThumbnailUrl(),
            view.getCaptureDate(),
            view.getLocation(),
            view.getUploadedBy().getId(),
            view.getUploadedBy().getFirstName() + " " + view.getUploadedBy().getLastName(),
            view.getUploadedAt(),
            view.getIsActive(),
            view.getViewCount()
        );
    }
}

