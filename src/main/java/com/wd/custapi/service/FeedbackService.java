package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FeedbackService {
    
    private final FeedbackFormRepository feedbackFormRepository;
    private final FeedbackResponseRepository feedbackResponseRepository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    private final ActivityFeedService activityFeedService;
    
    public FeedbackService(FeedbackFormRepository feedbackFormRepository,
                           FeedbackResponseRepository feedbackResponseRepository,
                           ProjectRepository projectRepository,
                           CustomerUserRepository userRepository,
                           ActivityFeedService activityFeedService) {
        this.feedbackFormRepository = feedbackFormRepository;
        this.feedbackResponseRepository = feedbackResponseRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.activityFeedService = activityFeedService;
    }
    
    @Transactional
    public FeedbackFormDto createForm(Long projectId, FeedbackFormRequest request, Long userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        CustomerUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        FeedbackForm form = new FeedbackForm();
        form.setProject(project);
        form.setTitle(request.title());
        form.setDescription(request.description());
        form.setFormType(request.formType());
        form.setCreatedBy(user);
        
        form = feedbackFormRepository.save(form);
        return toFormDto(form, null);
    }
    
    @Transactional
    public FeedbackResponseDto submitResponse(Long formId, FeedbackResponseRequest request, Long userId) {
        FeedbackForm form = feedbackFormRepository.findById(formId)
            .orElseThrow(() -> new RuntimeException("Feedback form not found"));
        
        CustomerUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if user already submitted
        feedbackResponseRepository.findByFormIdAndCustomerId(formId, userId)
            .ifPresent(existing -> {
                throw new RuntimeException("You have already submitted feedback for this form");
            });
        
        FeedbackResponse response = new FeedbackResponse();
        response.setForm(form);
        response.setProject(form.getProject());
        response.setCustomer(user);
        response.setRating(request.rating());
        response.setComments(request.comments());
        response.setResponseData(request.responseData());
        
        response = feedbackResponseRepository.save(response);
        
        // Create activity feed
        activityFeedService.createActivity(form.getProject().getId(), "FEEDBACK_SUBMITTED", 
            "Feedback submitted: " + form.getTitle(), response.getId(), userId);
        
        return toResponseDto(response);
    }
    
    public List<FeedbackFormDto> getProjectForms(Long projectId, Long userId) {
        List<FeedbackForm> forms = feedbackFormRepository.findByProjectIdAndIsActiveTrue(projectId);
        
        return forms.stream().map(form -> {
            boolean isCompleted = feedbackResponseRepository
                .findByFormIdAndCustomerId(form.getId(), userId)
                .isPresent();
            return toFormDto(form, isCompleted);
        }).collect(Collectors.toList());
    }
    
    public List<FeedbackResponseDto> getFormResponses(Long formId) {
        return feedbackResponseRepository.findByFormId(formId)
            .stream()
            .map(this::toResponseDto)
            .collect(Collectors.toList());
    }
    
    private FeedbackFormDto toFormDto(FeedbackForm form, Boolean isCompleted) {
        return new FeedbackFormDto(
            form.getId(),
            form.getProject().getId(),
            form.getTitle(),
            form.getDescription(),
            form.getFormType(),
            form.getCreatedBy().getId(),
            form.getCreatedBy().getFirstName() + " " + form.getCreatedBy().getLastName(),
            form.getCreatedAt(),
            form.getIsActive(),
            isCompleted
        );
    }
    
    private FeedbackResponseDto toResponseDto(FeedbackResponse response) {
        return new FeedbackResponseDto(
            response.getId(),
            response.getForm().getId(),
            response.getForm().getTitle(),
            response.getProject().getId(),
            response.getCustomer().getId(),
            response.getCustomer().getFirstName() + " " + response.getCustomer().getLastName(),
            response.getRating(),
            response.getComments(),
            response.getResponseData(),
            response.getSubmittedAt(),
            response.getIsCompleted()
        );
    }
}

