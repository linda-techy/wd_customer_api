package com.wd.custapi.service;

import com.wd.custapi.dto.DashboardDto;
import com.wd.custapi.exception.CustomerApiException;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.ProjectDocument;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.ProjectDocumentRepository;
import com.wd.custapi.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private static final String ROLE_VIEWER = "VIEWER";
    private static final String ROLE_ADMIN = "ADMIN";

    private final CustomerUserRepository customerUserRepository;

    private final ProjectRepository projectRepository;

    private final ProjectDocumentRepository projectDocumentRepository;

    private final com.wd.custapi.repository.ProjectDesignStepRepository projectDesignStepRepository;

    private final com.wd.custapi.repository.ActivityFeedRepository activityFeedRepository;

    private final com.wd.custapi.repository.PaymentScheduleRepository paymentScheduleRepository;

    private final com.wd.custapi.repository.ProjectMilestoneRepository projectMilestoneRepository;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    @SuppressWarnings("java:S6813")   // self-injection requires field injection (constructor would cycle)
    private DashboardService self;

    public DashboardService(CustomerUserRepository customerUserRepository,
                            ProjectRepository projectRepository,
                            ProjectDocumentRepository projectDocumentRepository,
                            com.wd.custapi.repository.ProjectDesignStepRepository projectDesignStepRepository,
                            com.wd.custapi.repository.ActivityFeedRepository activityFeedRepository,
                            com.wd.custapi.repository.PaymentScheduleRepository paymentScheduleRepository,
                            com.wd.custapi.repository.ProjectMilestoneRepository projectMilestoneRepository) {
        this.customerUserRepository = customerUserRepository;
        this.projectRepository = projectRepository;
        this.projectDocumentRepository = projectDocumentRepository;
        this.projectDesignStepRepository = projectDesignStepRepository;
        this.activityFeedRepository = activityFeedRepository;
        this.paymentScheduleRepository = paymentScheduleRepository;
        this.projectMilestoneRepository = projectMilestoneRepository;
    }

    // ... existing code ...

    /**
     * Returns the business role name for a customer user (e.g. "CUSTOMER", "ARCHITECT", "VIEWER").
     * Falls back to "VIEWER" if the user or role is not found.
     */
    public String getUserRole(String email) {
        return customerUserRepository.findByEmail(email)
                .map(u -> u.getRole() != null ? u.getRole().getName() : ROLE_VIEWER)
                .orElse(ROLE_VIEWER);
    }

    @Transactional(readOnly = true)
    public DashboardDto getCustomerDashboard(String email) {
        try {
            CustomerUser user = customerUserRepository.findByEmail(email)
                    .orElseThrow(() -> new CustomerApiException("Customer user not found"));

            boolean isAdmin = user.getRole() != null && ROLE_ADMIN.equalsIgnoreCase(user.getRole().getName());
            List<Project> userProjects = loadDashboardProjects(isAdmin, email);

            // Build dashboard data
            DashboardDto.UserSummary userSummary = new DashboardDto.UserSummary(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole() != null ? user.getRole().getName() : "UNKNOWN");

            DashboardDto.ProjectSummary projectSummary = buildProjectSummary(userProjects);
            List<DashboardDto.RecentActivity> recentActivities = buildRecentActivities(userProjects);

            // Financial stats (payment totals) are only shown to primary CUSTOMER and ADMIN roles.
            // ARCHITECT, INTERIOR_DESIGNER, SITE_ENGINEER, VIEWER see zeroed-out stats.
            String userRole = user.getRole() != null ? user.getRole().getName() : ROLE_VIEWER;
            DashboardDto.QuickStats quickStats;
            if ("CUSTOMER".equalsIgnoreCase(userRole) || ROLE_ADMIN.equalsIgnoreCase(userRole)) {
                quickStats = buildQuickStats(userProjects);
            } else {
                quickStats = new DashboardDto.QuickStats(0L, 0L, 0L, 0.0, 0.0);
            }

            return new DashboardDto(userSummary, projectSummary, recentActivities, quickStats);
        } catch (Exception e) {
            // Log full detail internally but do NOT expose it to the caller
            logger.error("Error building dashboard for user: {}", email, e);
            throw new CustomerApiException("Error building dashboard. Please try again.", e);
        }
    }

    /**
     * Loads the projects shown on the dashboard. Tolerant of fetch failures —
     * returns an empty list (rather than failing the whole dashboard) if the
     * project query throws.
     */
    private List<Project> loadDashboardProjects(boolean isAdmin, String email) {
        try {
            if (isAdmin) {
                // Dashboard only shows 5 recent project cards — load 20 max.
                // Previously loaded 50 full entities; reduced to 20 to limit heap pressure.
                // Future improvement: replace with a ProjectSummaryProjection (id, name, code,
                // progress, projectPhase) once the projection interface is wired to the repo.
                // Admin browsing beyond 20 should use GET /api/dashboard/admin/projects (paginated).
                List<Project> userProjects = projectRepository.findRecentForAdmin(20);
                logger.info("Admin user {}: loaded {} recent projects for dashboard", email, userProjects.size());
                return userProjects;
            } else {
                List<Project> userProjects = projectRepository.findAllByCustomerEmail(email);
                logger.info("Found {} projects for user: {}", userProjects.size(), email);
                return userProjects;
            }
        } catch (Exception e) {
            logger.error("Error fetching projects for {}: {}", email, e.getMessage());
            return new ArrayList<>();
        }
    }

    private DashboardDto.ProjectSummary buildProjectSummary(List<Project> projects) {
        long totalProjects = projects.size();
        long activeProjects = projects.stream()
                .filter(p -> p.getEndDate() == null || p.getEndDate().isAfter(LocalDate.now()))
                .count();
        long completedProjects = totalProjects - activeProjects;

        List<Project> recent = projects.stream().limit(5).toList();
        List<DashboardDto.ProjectCard> recentProjects = toProjectCards(recent);

        return new DashboardDto.ProjectSummary(totalProjects, activeProjects, completedProjects, recentProjects);
    }

    /**
     * Converts a list of projects to ProjectCard DTOs in a single batch.
     * One query fetches design-progress for all projects; avoids N+1 per card.
     */
    private List<DashboardDto.ProjectCard> toProjectCards(List<Project> projects) {
        if (projects.isEmpty()) return Collections.emptyList();

        List<Long> ids = projects.stream().map(Project::getId).toList();
        Map<Long, Double> progressMap = new HashMap<>();
        try {
            List<Object[]> rows = projectDesignStepRepository.calculateDesignProgressBatch(ids);
            for (Object[] row : rows) {
                Long pid = ((Number) row[0]).longValue();
                Double prog = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
                progressMap.put(pid, prog);
            }
        } catch (Exception e) {
            logger.warn("Could not batch-load design progress: {}", e.getMessage());
        }

        return projects.stream()
                .map(p -> toProjectCard(p, progressMap.getOrDefault(p.getId(), 0.0)))
                .toList();
    }

    private DashboardDto.ProjectCard toProjectCard(Project project, double designProgress) {
        String status = determineProjectStatus(project);

        return new DashboardDto.ProjectCard(
                project.getId(),
                project.getProjectUuid().toString(),
                project.getName(),
                project.getCode(),
                project.getLocation(),
                project.getStartDate(),
                project.getEndDate(),
                status,
                project.getProgress() != null ? project.getProgress().doubleValue() : 0.0,
                project.getProjectPhase() != null ? project.getProjectPhase().name() : null,
                project.getProjectType(),
                project.getDesignPackage(),
                Boolean.TRUE.equals(project.getIsDesignAgreementSigned()),
                designProgress);
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

        try {
            List<Long> projectIds = projects.stream()
                    .map(Project::getId)
                    .toList();

            // Single bulk DB query — replaces old per-project N+1 loop
            org.springframework.data.domain.Pageable top10 =
                    org.springframework.data.domain.PageRequest.of(0, 10);
            List<com.wd.custapi.model.ActivityFeed> activityFeeds =
                    activityFeedRepository.findTop10ByProjectIdInOrderByCreatedAtDesc(projectIds, top10);

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
        }

        return activities;
    }

    /**
     * Uses DB-side aggregation instead of loading all payment rows into memory.
     * Accepts the already-resolved user and projects to avoid redundant DB hits.
     */
    private DashboardDto.QuickStats buildQuickStats(List<Project> userProjects) {
        try {
            if (userProjects.isEmpty()) {
                return new DashboardDto.QuickStats(0L, 0L, 0L, 0.0, 0.0);
            }

            List<Long> projectIds = userProjects.stream()
                    .map(Project::getId)
                    .toList();

            // Single DB aggregate query — no full row loading
            Object[] row = paymentScheduleRepository.getPaymentSummaryForProjects(projectIds);
            if (row == null || row[0] == null) {
                return new DashboardDto.QuickStats(0L, 0L, 0L, 0.0, 0.0);
            }

            long totalBills   = ((Number) row[0]).longValue();
            long pendingBills = ((Number) row[1]).longValue();
            long paidBills    = ((Number) row[2]).longValue();
            double totalAmount   = ((Number) row[3]).doubleValue();
            double pendingAmount = ((Number) row[4]).doubleValue();

            return new DashboardDto.QuickStats(totalBills, pendingBills, paidBills, totalAmount, pendingAmount);
        } catch (Exception e) {
            logger.warn("Error fetching payment statistics: {}", e.getMessage());
            return new DashboardDto.QuickStats(0L, 0L, 0L, 0.0, 0.0);
        }
    }

    /**
     * Paginated project list for admin users. Supports optional full-text search.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getAdminProjectsPaged(int page, int size, String search) {
        int safeSize = Math.clamp(size, 1, 100);
        int offset = Math.max(page, 0) * safeSize;
        boolean hasSearch = search != null && !search.trim().isEmpty();
        String q = hasSearch ? search.trim() : null;

        List<Project> projects = hasSearch
                ? projectRepository.searchForAdminPaged(q, safeSize, offset)
                : projectRepository.findAllForAdminPaged(safeSize, offset);
        long total = hasSearch
                ? projectRepository.countForAdminSearch(q)
                : projectRepository.countAllForAdmin();

        List<DashboardDto.ProjectCard> cards = toProjectCards(projects);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("content", cards);
        result.put("page", page);
        result.put("size", safeSize);
        result.put("totalElements", total);
        result.put("totalPages", (int) Math.ceil((double) total / safeSize));
        result.put("hasNext", offset + safeSize < total);
        return result;
    }

    private boolean isAdminByEmail(String email) {
        return customerUserRepository.findByEmail(email)
                .map(u -> u.getRole() != null && ROLE_ADMIN.equalsIgnoreCase(u.getRole().getName()))
                .orElse(false);
    }

    /**
     * Get all projects accessible by the current user.
     * Admin users see all projects; regular users see only their assigned projects.
     * Used by customer-facing controllers for authorization checks.
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Project getProjectByUuidAndEmail(String projectUuidStr, String email) {
        java.util.UUID projectUuid;
        try {
            projectUuid = java.util.UUID.fromString(projectUuidStr);
        } catch (IllegalArgumentException e) {
            throw new CustomerApiException("Invalid project UUID format: " + projectUuidStr);
        }
        Project project = isAdminByEmail(email)
                ? projectRepository.findByProjectUuid(projectUuid)
                : projectRepository.findByProjectUuidAndCustomerEmail(projectUuid, email);
        if (project == null) {
            throw new CustomerApiException("Project not found or access denied");
        }
        return project;
    }

    @Transactional(readOnly = true)
    public Project getProjectByIdAndEmail(Long projectId, String email) {
        Project project = isAdminByEmail(email)
                ? projectRepository.findById(projectId).orElse(null)
                : projectRepository.findByIdAndCustomerEmail(projectId, email);
        if (project == null) {
            throw new CustomerApiException("Project not found or access denied");
        }
        return project;
    }

    // Get recent N projects for dashboard
    @Transactional(readOnly = true)
    public List<DashboardDto.ProjectCard> getRecentProjects(String email, int limit) {
        List<Project> projects = isAdminByEmail(email)
                ? projectRepository.findRecentForAdmin(limit)
                : projectRepository.findRecentByCustomerEmail(email, limit);
        return toProjectCards(projects);
    }

    // Search projects by name, code, or location
    @Transactional(readOnly = true)
    public List<DashboardDto.ProjectCard> searchProjects(String email, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return (self != null ? self : this).getRecentProjects(email, 5);
        }
        String term = searchTerm.trim();
        List<Project> projects = isAdminByEmail(email)
                ? projectRepository.searchForAdmin(term)
                : projectRepository.searchByCustomerEmailAndTerm(email, term);
        return toProjectCards(projects);
    }

    // Get detailed project information including progress and documents
    @Transactional(readOnly = true)
    public DashboardDto.ProjectDetails getProjectDetails(String projectUuidStr, String email) {
        // Parse UUID
        java.util.UUID projectUuid;
        try {
            projectUuid = java.util.UUID.fromString(projectUuidStr);
        } catch (IllegalArgumentException e) {
            throw new CustomerApiException("Invalid project UUID format: " + projectUuidStr);
        }

        // Get project (admin can access any; others must be in project_members)
        Project project;
        if (isAdminByEmail(email)) {
            project = projectRepository.findByProjectUuid(projectUuid);
        } else {
            project = projectRepository.findByProjectUuidAndCustomerEmail(projectUuid, email);
        }
        if (project == null) {
            throw new CustomerApiException("Project not found or access denied");
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
        details.setProgress(project.getProgress() != null ? project.getProgress().doubleValue() : 0.0);
        details.setStatus(determineProjectStatus(project));
        details.setPhase(project.getProjectPhase() != null ? project.getProjectPhase().name() : null);
        details.setProjectType(project.getProjectType());
        details.setDesignPackage(project.getDesignPackage());
        details.setDesignAgreementSigned(
                Boolean.TRUE.equals(project.getIsDesignAgreementSigned()));

        Double calculatedDesignProgress = projectDesignStepRepository.calculateDesignProgress(project.getId());
        if (calculatedDesignProgress == null) {
            calculatedDesignProgress = 0.0;
        }
        details.setDesignProgress(calculatedDesignProgress);

        details.setSqFeet(project.getSqFeet());
        details.setContractValueDisplay(
                com.wd.custapi.util.ContractValueFormatter.formatINR(project.getBudget()));
        details.setEstimatedCompletionDate(
                project.getEndDate() != null ? project.getEndDate().toString() : null);
        details.setState(null); // State not in Project entity yet
        details.setCreatedBy(null); // CreatedBy not in Project entity yet

        // Get project documents
        List<ProjectDocument> documents = projectDocumentRepository
                .findByReferenceIdAndReferenceTypeAndIsActiveTrue(project.getId(), "PROJECT");
        List<DashboardDto.ProjectDocumentSummary> documentSummaries = documents.stream()
                .map(this::toDocumentSummary)
                .toList();
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
            throw new CustomerApiException("Invalid project UUID format: " + projectUuidStr);
        }

        // Validate design package value
        if (designPackage == null || designPackage.trim().isEmpty()) {
            throw new CustomerApiException("Design package cannot be empty");
        }

        String normalizedPackage = designPackage.trim().toLowerCase();
        if (!normalizedPackage.equals("custom") && !normalizedPackage.equals("premium")
                && !normalizedPackage.equals("bespoke")) {
            throw new CustomerApiException("Invalid design package. Must be: custom, premium, or bespoke");
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
            throw new CustomerApiException("Project not found or access denied");
        }

        // Update design package
        project.setDesignPackage(normalizedPackage);
        projectRepository.save(project);

        // Return updated project details
        return (self != null ? self : this).getProjectDetails(projectUuidStr, email);
    }

    // S125: the block below is genuine explanatory prose about polymorphic
    // uploader-id resolution, not commented-out code.
    @SuppressWarnings("java:S125")
    private DashboardDto.ProjectDocumentSummary toDocumentSummary(ProjectDocument doc) {
        String downloadUrl = "/api/storage/" + doc.getFilePath();
        // Uploader id is polymorphic (customer_users.id or portal_users.id);
        // Customer-side has no portal_users entity, so for portal uploads we
        // simply attribute to "Company". Customer-uploaded docs (future) get
        // resolved via CustomerUserRepository.
        String uploadedBy = "Company";
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
        progressData.setOverallProgress(project.getProgress() != null ? project.getProgress().doubleValue() : 0.0);

        populateScheduleProgress(progressData, project);
        progressData.setMilestones(loadProgressMilestones(project.getId()));

        return progressData;
    }

    /**
     * Fills total/elapsed/remaining day counts and the derived progress status.
     * When start or end date is missing, everything is zeroed and status is UNKNOWN.
     */
    private void populateScheduleProgress(DashboardDto.ProgressData progressData, Project project) {
        LocalDate startDate = project.getStartDate();
        LocalDate endDate = project.getEndDate();

        if (startDate == null || endDate == null) {
            progressData.setTotalDays(0);
            progressData.setDaysElapsed(0);
            progressData.setDaysRemaining(0);
            progressData.setProgressStatus("UNKNOWN");
            return;
        }

        LocalDate now = LocalDate.now();
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        long daysElapsed = ChronoUnit.DAYS.between(startDate, now);
        long daysRemaining = ChronoUnit.DAYS.between(now, endDate);

        progressData.setTotalDays((int) totalDays);
        progressData.setDaysElapsed((int) Math.max(0, daysElapsed));
        progressData.setDaysRemaining((int) Math.max(0, daysRemaining));

        double actualProgress = project.getProgress() != null ? project.getProgress().doubleValue() : 0;
        progressData.setProgressStatus(determineProgressStatus(totalDays, daysElapsed, daysRemaining, actualProgress));
    }

    /**
     * Derives the schedule status (DELAYED / AHEAD / ON_TRACK) by comparing actual
     * progress against the linear expected progress for the elapsed time.
     */
    private String determineProgressStatus(long totalDays, long daysElapsed, long daysRemaining, double actualProgress) {
        if (daysRemaining < 0) {
            return "DELAYED";
        }
        double expectedProgress = (double) daysElapsed / totalDays * 100;
        if (actualProgress >= expectedProgress + 5) {
            return "AHEAD";
        }
        if (actualProgress < expectedProgress - 10) {
            return "DELAYED";
        }
        return "ON_TRACK";
    }

    /**
     * Loads real milestones from project_milestones table (written by portal API, shared DB).
     * Sorted by due date ascending so the customer app's ScheduleScreen shows them in order.
     */
    private List<DashboardDto.ProgressMilestone> loadProgressMilestones(Long projectId) {
        List<com.wd.custapi.model.ProjectMilestone> dbMilestones =
                projectMilestoneRepository.findByProjectIdOrderByDueDateAsc(projectId);

        return dbMilestones.stream()
                .map(this::toProgressMilestone)
                .toList();
    }

    private DashboardDto.ProgressMilestone toProgressMilestone(com.wd.custapi.model.ProjectMilestone m) {
        DashboardDto.ProgressMilestone pm = new DashboardDto.ProgressMilestone();
        pm.setName(m.getName());
        pm.setProgressPercentage(
                m.getCompletionPercentage() != null
                        ? m.getCompletionPercentage().doubleValue()
                        : 0.0);
        pm.setTargetDate(m.getDueDate());
        pm.setCompletedDate(m.getCompletedDate());
        pm.setStatus(m.getStatus() != null ? m.getStatus() : "PENDING");
        return pm;
    }
}
