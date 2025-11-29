package com.wd.custapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardDto {

    private UserSummary user;
    private ProjectSummary projects;
    private List<RecentActivity> recentActivities;
    private QuickStats quickStats;

    // Constructors
    public DashboardDto() {
    }

    public DashboardDto(UserSummary user, ProjectSummary projects, List<RecentActivity> recentActivities,
            QuickStats quickStats) {
        this.user = user;
        this.projects = projects;
        this.recentActivities = recentActivities;
        this.quickStats = quickStats;
    }

    // Getters and Setters
    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    public ProjectSummary getProjects() {
        return projects;
    }

    public void setProjects(ProjectSummary projects) {
        this.projects = projects;
    }

    public List<RecentActivity> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<RecentActivity> recentActivities) {
        this.recentActivities = recentActivities;
    }

    public QuickStats getQuickStats() {
        return quickStats;
    }

    public void setQuickStats(QuickStats quickStats) {
        this.quickStats = quickStats;
    }

    // Inner classes
    public static class UserSummary {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
        private String fullName;

        public UserSummary() {
        }

        public UserSummary(Long id, String email, String firstName, String lastName, String role) {
            this.id = id;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.role = role;
            this.fullName = firstName + " " + lastName;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }

    public static class ProjectSummary {
        private Long totalProjects;
        private Long activeProjects;
        private Long completedProjects;
        private List<ProjectCard> recentProjects;

        public ProjectSummary() {
        }

        public ProjectSummary(Long totalProjects, Long activeProjects, Long completedProjects,
                List<ProjectCard> recentProjects) {
            this.totalProjects = totalProjects;
            this.activeProjects = activeProjects;
            this.completedProjects = completedProjects;
            this.recentProjects = recentProjects;
        }

        // Getters and Setters
        public Long getTotalProjects() {
            return totalProjects;
        }

        public void setTotalProjects(Long totalProjects) {
            this.totalProjects = totalProjects;
        }

        public Long getActiveProjects() {
            return activeProjects;
        }

        public void setActiveProjects(Long activeProjects) {
            this.activeProjects = activeProjects;
        }

        public Long getCompletedProjects() {
            return completedProjects;
        }

        public void setCompletedProjects(Long completedProjects) {
            this.completedProjects = completedProjects;
        }

        public List<ProjectCard> getRecentProjects() {
            return recentProjects;
        }

        public void setRecentProjects(List<ProjectCard> recentProjects) {
            this.recentProjects = recentProjects;
        }
    }

    public static class ProjectCard {
        private Long id;
        private String name;
        private String code;
        private String location;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private Double progress;
        private String phase;

        public ProjectCard() {
        }

        public ProjectCard(Long id, String name, String code, String location, LocalDate startDate, LocalDate endDate,
                String status, Double progress, String phase) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.location = location;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.progress = progress;
            this.phase = phase;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Double getProgress() {
            return progress;
        }

        public void setProgress(Double progress) {
            this.progress = progress;
        }

        public String getPhase() {
            return phase;
        }

        public void setPhase(String phase) {
            this.phase = phase;
        }
    }

    public static class RecentActivity {
        private String type;
        private String description;
        private String timestamp;
        private Long projectId;
        private String projectName;

        public RecentActivity() {
        }

        public RecentActivity(String type, String description, String timestamp, Long projectId, String projectName) {
            this.type = type;
            this.description = description;
            this.timestamp = timestamp;
            this.projectId = projectId;
            this.projectName = projectName;
        }

        // Getters and Setters
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public Long getProjectId() {
            return projectId;
        }

        public void setProjectId(Long projectId) {
            this.projectId = projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }
    }

    public static class QuickStats {
        private Long totalBills;
        private Long pendingBills;
        private Long paidBills;
        private Double totalAmount;
        private Double pendingAmount;

        public QuickStats() {
        }

        public QuickStats(Long totalBills, Long pendingBills, Long paidBills, Double totalAmount,
                Double pendingAmount) {
            this.totalBills = totalBills;
            this.pendingBills = pendingBills;
            this.paidBills = paidBills;
            this.totalAmount = totalAmount;
            this.pendingAmount = pendingAmount;
        }

        // Getters and Setters
        public Long getTotalBills() {
            return totalBills;
        }

        public void setTotalBills(Long totalBills) {
            this.totalBills = totalBills;
        }

        public Long getPendingBills() {
            return pendingBills;
        }

        public void setPendingBills(Long pendingBills) {
            this.pendingBills = pendingBills;
        }

        public Long getPaidBills() {
            return paidBills;
        }

        public void setPaidBills(Long paidBills) {
            this.paidBills = paidBills;
        }

        public Double getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(Double totalAmount) {
            this.totalAmount = totalAmount;
        }

        public Double getPendingAmount() {
            return pendingAmount;
        }

        public void setPendingAmount(Double pendingAmount) {
            this.pendingAmount = pendingAmount;
        }
    }

    // Project Details DTO - Used when viewing a single project
    public static class ProjectDetails {
        private Long id;
        private String name;
        private String code;
        private String location;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate createdAt;
        private LocalDate updatedAt;
        private Double progress;
        private String status;
        private String phase;
        private String state;
        private String createdBy;
        private String responsiblePerson;
        private Double sqFeet;
        private Double leadId;
        private List<ProjectDocumentSummary> documents;
        private ProgressData progressData;

        public ProjectDetails() {
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        public LocalDate getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDate createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDate getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDate updatedAt) {
            this.updatedAt = updatedAt;
        }

        public Double getProgress() {
            return progress;
        }

        public void setProgress(Double progress) {
            this.progress = progress;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @JsonProperty("projectPhase")
        public String getPhase() {
            return phase;
        }

        public void setPhase(String phase) {
            this.phase = phase;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getCreatedBy() {
            return createdBy;
        }

        public void setCreatedBy(String createdBy) {
            this.createdBy = createdBy;
        }

        public String getResponsiblePerson() {
            return responsiblePerson;
        }

        public void setResponsiblePerson(String responsiblePerson) {
            this.responsiblePerson = responsiblePerson;
        }

        public Double getSqFeet() {
            return sqFeet;
        }

        public void setSqFeet(Double sqFeet) {
            this.sqFeet = sqFeet;
        }

        public Double getLeadId() {
            return leadId;
        }

        public void setLeadId(Double leadId) {
            this.leadId = leadId;
        }

        public List<ProjectDocumentSummary> getDocuments() {
            return documents;
        }

        public void setDocuments(List<ProjectDocumentSummary> documents) {
            this.documents = documents;
        }

        public ProgressData getProgressData() {
            return progressData;
        }

        public void setProgressData(ProgressData progressData) {
            this.progressData = progressData;
        }
    }

    // Project Document Summary for listing
    public static class ProjectDocumentSummary {
        private Long id;
        private String filename;
        private String downloadUrl;
        private Long fileSize;
        private String fileType;
        private String categoryName;
        private LocalDateTime uploadDate;
        private String uploadedBy;

        public ProjectDocumentSummary() {
        }

        public ProjectDocumentSummary(Long id, String filename, String downloadUrl, Long fileSize,
                String fileType, String categoryName, LocalDateTime uploadDate, String uploadedBy) {
            this.id = id;
            this.filename = filename;
            this.downloadUrl = downloadUrl;
            this.fileSize = fileSize;
            this.fileType = fileType;
            this.categoryName = categoryName;
            this.uploadDate = uploadDate;
            this.uploadedBy = uploadedBy;
        }

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public LocalDateTime getUploadDate() {
            return uploadDate;
        }

        public void setUploadDate(LocalDateTime uploadDate) {
            this.uploadDate = uploadDate;
        }

        public String getUploadedBy() {
            return uploadedBy;
        }

        public void setUploadedBy(String uploadedBy) {
            this.uploadedBy = uploadedBy;
        }
    }

    // Progress Data for charts and visualization
    public static class ProgressData {
        private Double overallProgress;
        private Integer daysRemaining;
        private Integer totalDays;
        private Integer daysElapsed;
        private String progressStatus; // ON_TRACK, AHEAD, DELAYED
        private List<ProgressMilestone> milestones;

        public ProgressData() {
        }

        // Getters and Setters
        public Double getOverallProgress() {
            return overallProgress;
        }

        public void setOverallProgress(Double overallProgress) {
            this.overallProgress = overallProgress;
        }

        public Integer getDaysRemaining() {
            return daysRemaining;
        }

        public void setDaysRemaining(Integer daysRemaining) {
            this.daysRemaining = daysRemaining;
        }

        public Integer getTotalDays() {
            return totalDays;
        }

        public void setTotalDays(Integer totalDays) {
            this.totalDays = totalDays;
        }

        public Integer getDaysElapsed() {
            return daysElapsed;
        }

        public void setDaysElapsed(Integer daysElapsed) {
            this.daysElapsed = daysElapsed;
        }

        public String getProgressStatus() {
            return progressStatus;
        }

        public void setProgressStatus(String progressStatus) {
            this.progressStatus = progressStatus;
        }

        public List<ProgressMilestone> getMilestones() {
            return milestones;
        }

        public void setMilestones(List<ProgressMilestone> milestones) {
            this.milestones = milestones;
        }
    }

    // Progress Milestone for timeline
    public static class ProgressMilestone {
        private String name;
        private Double progressPercentage;
        private LocalDate targetDate;
        private LocalDate completedDate;
        private String status; // COMPLETED, IN_PROGRESS, PENDING

        public ProgressMilestone() {
        }

        public ProgressMilestone(String name, Double progressPercentage, LocalDate targetDate, LocalDate completedDate,
                String status) {
            this.name = name;
            this.progressPercentage = progressPercentage;
            this.targetDate = targetDate;
            this.completedDate = completedDate;
            this.status = status;
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getProgressPercentage() {
            return progressPercentage;
        }

        public void setProgressPercentage(Double progressPercentage) {
            this.progressPercentage = progressPercentage;
        }

        public LocalDate getTargetDate() {
            return targetDate;
        }

        public void setTargetDate(LocalDate targetDate) {
            this.targetDate = targetDate;
        }

        public LocalDate getCompletedDate() {
            return completedDate;
        }

        public void setCompletedDate(LocalDate completedDate) {
            this.completedDate = completedDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
