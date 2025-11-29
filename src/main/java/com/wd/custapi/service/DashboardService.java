package com.wd.custapi.service;

import com.wd.custapi.dto.DashboardDto;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.ProjectDocument;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.ProjectDocumentRepository;
import com.wd.custapi.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    
    @Autowired
    private CustomerUserRepository customerUserRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private ProjectDocumentRepository projectDocumentRepository;
    
    public DashboardDto getCustomerDashboard(String email) {
        try {
            CustomerUser user = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer user not found: " + email));
            
            List<Project> userProjects;
            try {
                userProjects = projectRepository.findAllByCustomerEmail(email);
                System.out.println("Found " + userProjects.size() + " projects for user: " + email);
            } catch (Exception e) {
                System.err.println("Error fetching projects for " + email + ": " + e.getMessage());
                userProjects = new ArrayList<>();
            }
            
            // Build dashboard data
            DashboardDto.UserSummary userSummary = new DashboardDto.UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() != null ? user.getRole().getName() : "UNKNOWN"
            );
            
            DashboardDto.ProjectSummary projectSummary = buildProjectSummary(userProjects);
            List<DashboardDto.RecentActivity> recentActivities = buildRecentActivities(userProjects);
            DashboardDto.QuickStats quickStats = buildQuickStats(user.getId());
            
            return new DashboardDto(userSummary, projectSummary, recentActivities, quickStats);
        } catch (Exception e) {
            throw new RuntimeException("Error building dashboard for user: " + email + ", Error: " + e.getMessage(), e);
        }
    }
    
    private DashboardDto.ProjectSummary buildProjectSummary(List<Project> projects) {
        long totalProjects = projects.size();
        long activeProjects = projects.stream()
            .filter(p -> p.getEndDate() == null || p.getEndDate().isAfter(LocalDate.now()))
            .count();
        long completedProjects = totalProjects - activeProjects;
        
        // Get recent projects (last 5)
        List<DashboardDto.ProjectCard> recentProjects = projects.stream()
            .limit(5)
            .map(this::toProjectCard)
            .collect(Collectors.toList());
        
        return new DashboardDto.ProjectSummary(totalProjects, activeProjects, completedProjects, recentProjects);
    }
    
    private DashboardDto.ProjectCard toProjectCard(Project project) {
        String status = determineProjectStatus(project);
        DashboardDto.ProjectCard card = new DashboardDto.ProjectCard(
            project.getId(),
            project.getName(),
            project.getCode(),
            project.getLocation(),
            project.getStartDate(),
            project.getEndDate(),
            status,
            project.getProgress(),
            project.getProjectPhase()
        );
        return card;
    }
    
    private String determineProjectStatus(Project project) {
        if (project.getEndDate() == null) {
            return "ACTIVE";
        } else if (project.getEndDate().isBefore(LocalDate.now())) {
            return "COMPLETED";
        } else {
            return "ACTIVE";
        }
    }
    
    private List<DashboardDto.RecentActivity> buildRecentActivities(List<Project> projects) {
        List<DashboardDto.RecentActivity> activities = new ArrayList<>();
        
        // Mock recent activities - in real app, these would come from an activity log
        for (Project project : projects.stream().limit(3).collect(Collectors.toList())) {
            activities.add(new DashboardDto.RecentActivity(
                "PROJECT_UPDATE",
                "Project " + project.getName() + " was updated",
                LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
                project.getId(),
                project.getName()
            ));
        }
        
        return activities;
    }
    
    private DashboardDto.QuickStats buildQuickStats(Long customerId) {
        // Mock stats - in real app, these would come from bills/payments tables
        return new DashboardDto.QuickStats(
            10L,  // totalBills
            3L,   // pendingBills
            7L,   // paidBills
            150000.0,  // totalAmount
            45000.0    // pendingAmount
        );
    }
    
    // Get recent N projects for dashboard
    public List<DashboardDto.ProjectCard> getRecentProjects(String email, int limit) {
        List<Project> projects = projectRepository.findRecentByCustomerEmail(email, limit);
        return projects.stream()
            .map(this::toProjectCard)
            .collect(Collectors.toList());
    }
    
    // Search projects by name, code, or location
    public List<DashboardDto.ProjectCard> searchProjects(String email, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            // If no search term, return recent 5 projects
            return getRecentProjects(email, 5);
        }
        
        List<Project> projects = projectRepository.searchByCustomerEmailAndTerm(email, searchTerm.trim());
        return projects.stream()
            .map(this::toProjectCard)
            .collect(Collectors.toList());
    }
    
    // Get detailed project information including progress and documents
    public DashboardDto.ProjectDetails getProjectDetails(Long projectId, String email) {
        // Get project (ensures user has access to this project)
        Project project = projectRepository.findByIdAndCustomerEmail(projectId, email);
        if (project == null) {
            throw new RuntimeException("Project not found or access denied");
        }
        
        // Build project details
        DashboardDto.ProjectDetails details = new DashboardDto.ProjectDetails();
        details.setId(project.getId());
        details.setName(project.getName());
        details.setCode(project.getCode());
        details.setLocation(project.getLocation());
        details.setStartDate(project.getStartDate());
        details.setEndDate(project.getEndDate());
        details.setProgress(project.getProgress());
        details.setStatus(determineProjectStatus(project));
        details.setPhase(project.getProjectPhase()); // Set project phase from database
        // Note: createdAt, updatedAt, state, createdBy, responsiblePerson, sqFeet, leadId 
        // are not in the current Project model - add them to the model if needed
        
        // Get project documents
        List<ProjectDocument> documents = projectDocumentRepository.findByProjectIdAndIsActiveTrue(projectId);
        List<DashboardDto.ProjectDocumentSummary> documentSummaries = documents.stream()
            .map(this::toDocumentSummary)
            .collect(Collectors.toList());
        details.setDocuments(documentSummaries);
        
        // Build progress data
        DashboardDto.ProgressData progressData = buildProgressData(project);
        details.setProgressData(progressData);
        
        return details;
    }
    
    private DashboardDto.ProjectDocumentSummary toDocumentSummary(ProjectDocument doc) {
        // Generate full download URL - goes through authenticated /api/storage/ endpoint
        String downloadUrl = "https://cust-api.walldotbuilders.com/api/storage/" + doc.getFilePath();
        String uploadedBy = doc.getUploadedBy() != null 
            ? doc.getUploadedBy().getFirstName() + " " + doc.getUploadedBy().getLastName() 
            : "Unknown";
        String categoryName = doc.getCategory() != null ? doc.getCategory().getName() : "Uncategorized";
        
        return new DashboardDto.ProjectDocumentSummary(
            doc.getId(),
            doc.getFilename(),
            downloadUrl,
            doc.getFileSize(),
            doc.getFileType(),
            categoryName,
            doc.getUploadDate(),
            uploadedBy
        );
    }
    
    private DashboardDto.ProgressData buildProgressData(Project project) {
        DashboardDto.ProgressData progressData = new DashboardDto.ProgressData();
        progressData.setOverallProgress(project.getProgress());
        
        // Calculate days
        LocalDate now = LocalDate.now();
        LocalDate startDate = project.getStartDate();
        LocalDate endDate = project.getEndDate();
        
        if (startDate != null && endDate != null) {
            long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
            long daysElapsed = ChronoUnit.DAYS.between(startDate, now);
            long daysRemaining = ChronoUnit.DAYS.between(now, endDate);
            
            progressData.setTotalDays((int) totalDays);
            progressData.setDaysElapsed((int) Math.max(0, daysElapsed));
            progressData.setDaysRemaining((int) Math.max(0, daysRemaining));
            
            // Determine progress status
            if (daysRemaining < 0) {
                progressData.setProgressStatus("DELAYED");
            } else {
                double expectedProgress = (double) daysElapsed / totalDays * 100;
                double actualProgress = project.getProgress() != null ? project.getProgress() : 0;
                
                if (actualProgress >= expectedProgress + 5) {
                    progressData.setProgressStatus("AHEAD");
                } else if (actualProgress < expectedProgress - 10) {
                    progressData.setProgressStatus("DELAYED");
                } else {
                    progressData.setProgressStatus("ON_TRACK");
                }
            }
        } else {
            progressData.setTotalDays(0);
            progressData.setDaysElapsed(0);
            progressData.setDaysRemaining(0);
            progressData.setProgressStatus("UNKNOWN");
        }
        
        // Build mock milestones (in real app, these would come from a milestones table)
        List<DashboardDto.ProgressMilestone> milestones = new ArrayList<>();
        if (startDate != null && endDate != null) {
            long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
            
            milestones.add(new DashboardDto.ProgressMilestone(
                "Foundation", 25.0, 
                startDate.plusDays(totalDays / 4), 
                startDate.plusDays(totalDays / 4),
                "COMPLETED"
            ));
            
            milestones.add(new DashboardDto.ProgressMilestone(
                "Structure", 50.0,
                startDate.plusDays(totalDays / 2),
                null,
                project.getProgress() >= 50 ? "COMPLETED" : "IN_PROGRESS"
            ));
            
            milestones.add(new DashboardDto.ProgressMilestone(
                "Finishes", 75.0,
                startDate.plusDays(totalDays * 3 / 4),
                null,
                project.getProgress() >= 75 ? "IN_PROGRESS" : "PENDING"
            ));
            
            milestones.add(new DashboardDto.ProgressMilestone(
                "Completion", 100.0,
                endDate,
                null,
                project.getProgress() >= 100 ? "COMPLETED" : "PENDING"
            ));
        }
        progressData.setMilestones(milestones);
        
        return progressData;
    }
}
