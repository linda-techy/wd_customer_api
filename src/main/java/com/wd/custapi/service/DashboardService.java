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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectDocumentRepository projectDocumentRepository;

    @Autowired
    private com.wd.custapi.repository.ProjectDesignStepRepository projectDesignStepRepository;

    @Autowired
    private com.wd.custapi.repository.ActivityFeedRepository activityFeedRepository;

    @Autowired
    private com.wd.custapi.repository.PaymentScheduleRepository paymentScheduleRepository;

    // ... existing code ...

    public DashboardDto getCustomerDashboard(String email) {
        try {
            CustomerUser user = customerUserRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Customer user not found: " + email));

            boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName());
            List<Project> userProjects;
            try {
                if (isAdmin) {
                    userProjects = projectRepository.findAllForAdmin();
                    logger.info("Admin user {}: found {} projects (all customer_projects)", email, userProjects.size());
                } else {
                    userProjects = projectRepository.findAllByCustomerEmail(email);
                    logger.info("Found {} projects for user: {}", userProjects.size(), email);
                }
            } catch (Exception e) {
                logger.error("Error fetching projects for {}: {}", email, e.getMessage());
                userProjects = new ArrayList<>();
            }

            // Build dashboard data
            DashboardDto.UserSummary userSummary = new DashboardDto.UserSummary(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole() != null ? user.getRole().getName() : "UNKNOWN");

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

        Double calculatedDesignProgress = projectDesignStepRepository.calculateDesignProgress(project.getId());
        if (calculatedDesignProgress == null) {
            calculatedDesignProgress = 0.0;
        }

        DashboardDto.ProjectCard card = new DashboardDto.ProjectCard(
                project.getId(),
                project.getProjectUuid().toString(),
                project.getName(),
                project.getCode(),
                project.getLocation(),
                project.getStartDate(),
                project.getEndDate(),
                status,
                project.getProgress(),
                project.getProjectPhase(),
                project.getDesignPackage(),
                project.getIsDesignAgreementSigned() != null
                        ? project.getIsDesignAgreementSigned()
                        : false,
                calculatedDesignProgress);

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

        if (projects.isEmpty()) {
            return activities;
        }

        // Query real activities from ActivityFeed for all user's projects
        try {
            List<Long> projectIds = projects.stream()
                    .map(Project::getId)
                    .collect(Collectors.toList());

            // Get activities for all projects, ordered by creation date descending
            List<com.wd.custapi.model.ActivityFeed> activityFeeds = new ArrayList<>();
            for (Long projectId : projectIds) {
                List<com.wd.custapi.model.ActivityFeed> projectActivities = 
                        activityFeedRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
                activityFeeds.addAll(projectActivities);
            }

            // Sort by createdAt descending and limit to 10 most recent
            activityFeeds.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            activityFeeds = activityFeeds.stream()
                    .limit(10)
                    .collect(Collectors.toList());

            // Convert to DTO format
            for (com.wd.custapi.model.ActivityFeed feed : activityFeeds) {
                String activityType = feed.getActivityType() != null 
                        ? feed.getActivityType().getName() 
                        : "ACTIVITY";
                String description = feed.getDescription() != null 
                        ? feed.getDescription() 
                        : feed.getTitle();
                String timestamp = feed.getCreatedAt().toLocalDate()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE);
                Long projectId = feed.getProject() != null ? feed.getProject().getId() : null;
                String projectName = feed.getProject() != null ? feed.getProject().getName() : "Unknown";

                activities.add(new DashboardDto.RecentActivity(
                        activityType,
                        description,
                        timestamp,
                        projectId,
                        projectName));
            }
        } catch (Exception e) {
            logger.warn("Error fetching recent activities: {}", e.getMessage());
            // Return empty list if there's an error - never return fake data
        }

        return activities;
    }

    private DashboardDto.QuickStats buildQuickStats(Long customerId) {
        // Get real payment statistics from PaymentSchedule
        try {
            // Get all projects for this customer
            CustomerUser user = customerUserRepository.findById(customerId)
                    .orElse(null);
            if (user == null) {
                return new DashboardDto.QuickStats(0L, 0L, 0L, 0.0, 0.0);
            }

            List<Project> userProjects;
            if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName())) {
                userProjects = projectRepository.findAllForAdmin();
            } else {
                userProjects = projectRepository.findAllByCustomerEmail(user.getEmail());
            }

            if (userProjects.isEmpty()) {
                return new DashboardDto.QuickStats(0L, 0L, 0L, 0.0, 0.0);
            }

            List<Long> projectIds = userProjects.stream()
                    .map(Project::getId)
                    .collect(Collectors.toList());

            // Query all payment schedules for these projects
            org.springframework.data.domain.Pageable pageable = 
                    org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE);
            org.springframework.data.domain.Page<com.wd.custapi.model.PaymentSchedule> paymentSchedules = 
                    paymentScheduleRepository.findByProjectIdIn(projectIds, pageable);

            long totalBills = paymentSchedules.getTotalElements();
            long pendingBills = paymentSchedules.getContent().stream()
                    .filter(ps -> ps.getStatus() != null && 
                            ("PENDING".equalsIgnoreCase(ps.getStatus()) || 
                             "OVERDUE".equalsIgnoreCase(ps.getStatus())))
                    .count();
            long paidBills = paymentSchedules.getContent().stream()
                    .filter(ps -> ps.getStatus() != null && 
                            "PAID".equalsIgnoreCase(ps.getStatus()))
                    .count();

            double totalAmount = paymentSchedules.getContent().stream()
                    .map(ps -> ps.getAmount() != null ? ps.getAmount().doubleValue() : 0.0)
                    .reduce(0.0, Double::sum);

            double pendingAmount = paymentSchedules.getContent().stream()
                    .filter(ps -> ps.getStatus() != null && 
                            ("PENDING".equalsIgnoreCase(ps.getStatus()) || 
                             "OVERDUE".equalsIgnoreCase(ps.getStatus())))
                    .map(ps -> ps.getAmount() != null ? ps.getAmount().doubleValue() : 0.0)
                    .reduce(0.0, Double::sum);

            return new DashboardDto.QuickStats(totalBills, pendingBills, paidBills, totalAmount, pendingAmount);
        } catch (Exception e) {
            logger.warn("Error fetching payment statistics: {}", e.getMessage());
            // Return zeros if there's an error - never return fake data
            return new DashboardDto.QuickStats(0L, 0L, 0L, 0.0, 0.0);
        }
    }

    private boolean isAdminByEmail(String email) {
        return customerUserRepository.findByEmail(email)
                .map(u -> u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().getName()))
                .orElse(false);
    }

    /**
     * Get all projects accessible by the current user.
     * Admin users see all projects; regular users see only their assigned projects.
     * Used by customer-facing controllers for authorization checks.
     */
    public List<Project> getProjectsForUser(String email) {
        logger.info("Fetching projects for user: {}", email);
        if (isAdminByEmail(email)) {
            List<Project> allProjects = projectRepository.findAllForAdmin();
            logger.info("Admin user {} retrieved {} projects", email, allProjects.size());
            return allProjects;
        }
        List<Project> userProjects = projectRepository.findAllByCustomerEmail(email);
        logger.info("Customer user {} retrieved {} projects", email, userProjects.size());
        return userProjects;
    }

    /**
     * Resolve project by UUID and current user email (with admin bypass).
     * Used by project module endpoints that accept projectUuid in the path.
     */
    public Project getProjectByUuidAndEmail(String projectUuidStr, String email) {
        java.util.UUID projectUuid;
        try {
            projectUuid = java.util.UUID.fromString(projectUuidStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid project UUID format: " + projectUuidStr);
        }
        Project project = isAdminByEmail(email)
                ? projectRepository.findByProjectUuid(projectUuid)
                : projectRepository.findByProjectUuidAndCustomerEmail(projectUuid, email);
        if (project == null) {
            throw new RuntimeException("Project not found or access denied");
        }
        return project;
    }

    // Get recent N projects for dashboard
    public List<DashboardDto.ProjectCard> getRecentProjects(String email, int limit) {
        List<Project> projects = isAdminByEmail(email)
                ? projectRepository.findRecentForAdmin(limit)
                : projectRepository.findRecentByCustomerEmail(email, limit);
        return projects.stream()
                .map(this::toProjectCard)
                .collect(Collectors.toList());
    }

    // Search projects by name, code, or location
    public List<DashboardDto.ProjectCard> searchProjects(String email, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getRecentProjects(email, 5);
        }
        String term = searchTerm.trim();
        List<Project> projects = isAdminByEmail(email)
                ? projectRepository.searchForAdmin(term)
                : projectRepository.searchByCustomerEmailAndTerm(email, term);
        return projects.stream()
                .map(this::toProjectCard)
                .collect(Collectors.toList());
    }

    // Get detailed project information including progress and documents
    public DashboardDto.ProjectDetails getProjectDetails(String projectUuidStr, String email) {
        // Parse UUID
        java.util.UUID projectUuid;
        try {
            projectUuid = java.util.UUID.fromString(projectUuidStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid project UUID format: " + projectUuidStr);
        }

        // Get project (admin can access any; others must be in project_members)
        Project project;
        if (isAdminByEmail(email)) {
            project = projectRepository.findByProjectUuid(projectUuid);
        } else {
            project = projectRepository.findByProjectUuidAndCustomerEmail(projectUuid, email);
        }
        if (project == null) {
            throw new RuntimeException("Project not found or access denied");
        }

        // Build project details
        DashboardDto.ProjectDetails details = new DashboardDto.ProjectDetails();
        details.setId(project.getId());
        details.setProjectUuid(project.getProjectUuid().toString());
        details.setName(project.getName());
        details.setCode(project.getCode());
        details.setLocation(project.getLocation());
        details.setStartDate(project.getStartDate());
        details.setEndDate(project.getEndDate());
        details.setProgress(project.getProgress());
        details.setStatus(determineProjectStatus(project));
        details.setPhase(project.getProjectPhase()); // Set project phase from database
        details.setDesignPackage(project.getDesignPackage());
        details.setDesignAgreementSigned(
                project.getIsDesignAgreementSigned() != null ? project.getIsDesignAgreementSigned() : false);

        Double calculatedDesignProgress = projectDesignStepRepository.calculateDesignProgress(project.getId());
        if (calculatedDesignProgress == null) {
            calculatedDesignProgress = 0.0;
        }
        details.setDesignProgress(calculatedDesignProgress);

        try {
            String sql = "SELECT sqfeet FROM customer_projects WHERE id = ?";
            Double rawSqFeet = jdbcTemplate.queryForObject(sql, Double.class, project.getId());
            logger.info("Project ID: {}, Entity SqFeet: {}, Raw DB SqFeet: {}", project.getId(), project.getSqFeet(),
                    rawSqFeet);
        } catch (Exception e) {
            logger.error("Failed to query raw sqfeet", e);
        }

        details.setSqFeet(project.getSqFeet());
        details.setState(null); // State not in Project entity yet
        details.setCreatedBy(null); // CreatedBy not in Project entity yetPerson, sqFeet, leadId
        // are not in the current Project model - add them to the model if needed

        // Get project documents
        List<ProjectDocument> documents = projectDocumentRepository
                .findByReferenceIdAndReferenceTypeAndIsActiveTrue(project.getId(), "PROJECT");
        List<DashboardDto.ProjectDocumentSummary> documentSummaries = documents.stream()
                .map(this::toDocumentSummary)
                .collect(Collectors.toList());
        details.setDocuments(documentSummaries);

        // Build progress data
        DashboardDto.ProgressData progressData = buildProgressData(project);
        details.setProgressData(progressData);

        return details;
    }

    // Update design package for a project
    // Update design package for a project
    @Transactional
    public DashboardDto.ProjectDetails updateDesignPackage(String projectUuidStr, String designPackage, String email) {
        // Parse UUID
        java.util.UUID projectUuid;
        try {
            projectUuid = java.util.UUID.fromString(projectUuidStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid project UUID format: " + projectUuidStr);
        }

        // Validate design package value
        if (designPackage == null || designPackage.trim().isEmpty()) {
            throw new RuntimeException("Design package cannot be empty");
        }

        String normalizedPackage = designPackage.trim().toLowerCase();
        if (!normalizedPackage.equals("custom") && !normalizedPackage.equals("premium")
                && !normalizedPackage.equals("bespoke")) {
            throw new RuntimeException("Invalid design package. Must be: custom, premium, or bespoke");
        }

        // Get project (admin can access any; others must be in project_members)
        Project project;
        if (isAdminByEmail(email)) {
            project = projectRepository.findByProjectUuid(projectUuid);
        } else {
            project = projectRepository.findByProjectUuidAndCustomerEmail(projectUuid, email);
        }
        if (project == null) {
            logger.warn("Project not found for uuid: {} and email: {}", projectUuid, email);
            throw new RuntimeException("Project not found or access denied");
        }

        // Update design package
        project.setDesignPackage(normalizedPackage);
        projectRepository.save(project);

        // Return updated project details
        return getProjectDetails(projectUuidStr, email);
    }

    private DashboardDto.ProjectDocumentSummary toDocumentSummary(ProjectDocument doc) {
        String downloadUrl = "/api/storage/" + doc.getFilePath();
        String uploadedBy = doc.getCreatedBy() != null
                ? doc.getCreatedBy().getFirstName() + " " + doc.getCreatedBy().getLastName()
                : "Company";
        String categoryName = doc.getCategory() != null ? doc.getCategory().getName() : "Uncategorized";

        return new DashboardDto.ProjectDocumentSummary(
                doc.getId(),
                doc.getFilename(),
                downloadUrl,
                doc.getFileSize(),
                doc.getFileType(),
                categoryName,
                doc.getCreatedAt(),
                uploadedBy);
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

        // TODO: Milestones feature needs to be implemented
        // Real milestones should come from a project_milestones table or project_phases table
        // For now, return empty list - never return fake/mock data in production
        List<DashboardDto.ProgressMilestone> milestones = new ArrayList<>();
        progressData.setMilestones(milestones);

        return progressData;
    }
}
