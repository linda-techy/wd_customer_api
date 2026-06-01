package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class QualityCheckService {
    
    private final QualityCheckRepository qualityCheckRepository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    private final ActivityFeedService activityFeedService;
    
    public QualityCheckService(QualityCheckRepository qualityCheckRepository,
                               ProjectRepository projectRepository,
                               CustomerUserRepository userRepository,
                               ActivityFeedService activityFeedService) {
        this.qualityCheckRepository = qualityCheckRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.activityFeedService = activityFeedService;
    }
    
    @Transactional
    public QualityCheckDto createQualityCheck(Long projectId, QualityCheckRequest request, Long userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        CustomerUser createdBy = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        QualityCheck qc = new QualityCheck();
        qc.setProject(project);
        qc.setTitle(request.title());
        qc.setDescription(request.description());
        qc.setSopReference(request.sopReference());
        qc.setPriority(QualityCheck.Priority.valueOf(request.priority()));
        qc.setCreatedBy(createdBy);
        
        if (request.assignedToId() != null) {
            CustomerUser assignedTo = userRepository.findById(request.assignedToId())
                .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            qc.setAssignedTo(assignedTo);
        }
        
        qc = qualityCheckRepository.save(qc);
        
        // Create activity feed
        activityFeedService.createActivity(projectId, "QUALITY_CHECK_ADDED", 
            "Quality check added: " + request.title(), qc.getId(), userId);
        
        return toDto(qc);
    }
    
    @Transactional
    public QualityCheckDto resolveQualityCheck(Long qcId, QualityCheckUpdateRequest request, Long userId) {
        QualityCheck qc = qualityCheckRepository.findById(qcId)
            .orElseThrow(() -> new RuntimeException("Quality check not found"));
        
        CustomerUser resolvedBy = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        qc.setStatus(QualityCheck.QualityCheckStatus.RESOLVED);
        qc.setResolvedAt(LocalDateTime.now());
        qc.setResolvedBy(resolvedBy);
        qc.setResolutionNotes(request.resolutionNotes());
        
        qc = qualityCheckRepository.save(qc);
        
        // Create activity feed
        activityFeedService.createActivity(qc.getProject().getId(), "QUALITY_CHECK_RESOLVED", 
            "Quality check resolved: " + qc.getTitle(), qc.getId(), userId);
        
        return toDto(qc);
    }
    
    @Transactional(readOnly = true)
    public List<QualityCheckDto> getQualityChecks(Long projectId, String status) {
        List<QualityCheck> checks;
        if (status != null) {
            QualityCheck.QualityCheckStatus qcStatus = QualityCheck.QualityCheckStatus.valueOf(status);
            checks = qualityCheckRepository.findByProjectIdAndStatusOrderByPriorityDescCreatedAtDesc(projectId, qcStatus);
        } else {
            checks = qualityCheckRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        }
        return checks.stream().map(this::toDto).toList();
    }
    
    private QualityCheckDto toDto(QualityCheck qc) {
        // QC is authored by site engineers (portal staff), which are not mirrored
        // into customer_users — so createdBy may be a dangling proxy. Resolve it
        // defensively (orphan/null -> "Site Engineer") and tolerate null
        // status/priority, so a staff-authored QC never 500s the customer list.
        Long createdById = null;
        String createdByName = "Site Engineer";
        try {
            if (qc.getCreatedBy() != null) {
                createdById = qc.getCreatedBy().getId();
                createdByName = qc.getCreatedBy().getFirstName() + " " + qc.getCreatedBy().getLastName();
            }
        } catch (RuntimeException ex) {
            createdByName = "Site Engineer";
        }
        return new QualityCheckDto(
            qc.getId(),
            qc.getProject() != null ? qc.getProject().getId() : null,
            qc.getTitle(),
            qc.getDescription(),
            qc.getSopReference(),
            qc.getStatus() != null ? qc.getStatus().name() : "ACTIVE",
            qc.getPriority() != null ? qc.getPriority().name() : "MEDIUM",
            qc.getAssignedTo() != null ? qc.getAssignedTo().getId() : null,
            qc.getAssignedTo() != null ? qc.getAssignedTo().getFirstName() + " " + qc.getAssignedTo().getLastName() : null,
            createdById,
            createdByName,
            qc.getCreatedAt(),
            qc.getResolvedAt(),
            qc.getResolvedBy() != null ? qc.getResolvedBy().getId() : null,
            qc.getResolvedBy() != null ? qc.getResolvedBy().getFirstName() + " " + qc.getResolvedBy().getLastName() : null,
            qc.getResolutionNotes()
        );
    }
}

