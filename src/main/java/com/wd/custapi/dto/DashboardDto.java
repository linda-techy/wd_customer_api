package com.wd.custapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    // Inner classes
    @Getter
    @Setter
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
    }

    @Getter
    @Setter
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
    }

    @Getter
    @Setter
    public static class ProjectCard {
        private Long id;
        private String projectUuid;
        private String name;
        private String code;
        private String location;
        private String startDate;
        private String endDate;
        private String status;
        private Double progress;
        private String projectPhase;
        private String projectType;
        private String designPackage;
        private boolean isDesignAgreementSigned;
        private Double designProgress;

        public ProjectCard() {
        }

        @SuppressWarnings("java:S107") // data carrier — many fields by design
        public ProjectCard(Long id, String projectUuid, String name, String code, String location, LocalDate startDate,
                LocalDate endDate,
                String status, Double progress, String projectPhase, String projectType, String designPackage,
                boolean isDesignAgreementSigned, Double designProgress) {
            this.id = id;
            this.projectUuid = projectUuid;
            this.name = name;
            this.code = code;
            this.location = location;
            this.startDate = startDate != null ? startDate.toString() : null;
            this.endDate = endDate != null ? endDate.toString() : null;
            this.status = status;
            this.progress = progress;
            this.projectPhase = projectPhase;
            this.projectType = projectType;
            this.designPackage = designPackage;
            this.isDesignAgreementSigned = isDesignAgreementSigned;
            this.designProgress = designProgress;
        }

        // Kept manual: boolean accessor — preserves Jackson JSON property name
        public boolean isDesignAgreementSigned() {
            return isDesignAgreementSigned;
        }

        public void setDesignAgreementSigned(boolean designAgreementSigned) {
            this.isDesignAgreementSigned = designAgreementSigned;
        }
    }

    @Getter
    @Setter
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
    }

    @Getter
    @Setter
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
    }

    // Project Details DTO - Used when viewing a single project
    @Getter
    @Setter
    public static class ProjectDetails {
        private Long id;
        private String projectUuid;
        private String name;
        private String code;
        private String location;
        private String startDate;
        private String endDate;
        private String status;
        private Double progress;
        private String phase;
        private String projectType;
        private String designPackage;
        private boolean isDesignAgreementSigned;
        private Double sqFeet;
        private Double designProgress;
        private String state;
        private String createdBy;
        private String responsiblePerson;
        private List<ProjectDocumentSummary> documents;
        private ProgressData progressData;
        private String contractValueDisplay;        // e.g. "₹1.20 Cr" or null
        private String estimatedCompletionDate;     // ISO date string or null

        public ProjectDetails() {
            // Default constructor required by Jackson for serialization
        }

        // Kept manual: @JsonProperty maps field `phase` -> JSON "projectPhase"
        @JsonProperty("projectPhase")
        public String getPhase() {
            return phase;
        }

        // Kept manual: boolean accessor + @JsonProperty preserve JSON property name
        @JsonProperty("isDesignAgreementSigned")
        public boolean isDesignAgreementSigned() {
            return isDesignAgreementSigned;
        }

        public void setDesignAgreementSigned(boolean designAgreementSigned) {
            isDesignAgreementSigned = designAgreementSigned;
        }

        // Kept manual: setStartDate/setEndDate are overloaded (String + LocalDate).
        // Lombok skips generating setStartDate(String)/setEndDate(String) because a
        // method with the same name already exists, so both String + LocalDate are kept.
        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        // Helper setters for LocalDate conversion
        public void setStartDate(LocalDate date) {
            this.startDate = date != null ? date.toString() : null;
        }

        public void setEndDate(LocalDate date) {
            this.endDate = date != null ? date.toString() : null;
        }
    }

    // Project Document Summary for listing
    @Getter
    @Setter
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

        @SuppressWarnings("java:S107") // data carrier — many fields by design
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
    }

    // Progress Data for charts and visualization
    @Getter
    @Setter
    public static class ProgressData {
        private Double overallProgress;
        private Integer daysRemaining;
        private Integer totalDays;
        private Integer daysElapsed;
        private String progressStatus; // ON_TRACK, AHEAD, DELAYED
        private List<ProgressMilestone> milestones;

        public ProgressData() {
            // Default constructor required by Jackson for serialization
        }
    }

    // Progress Milestone for timeline
    @Getter
    @Setter
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
    }
}
