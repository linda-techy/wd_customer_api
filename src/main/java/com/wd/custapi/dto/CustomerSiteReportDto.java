package com.wd.custapi.dto;

import com.wd.custapi.model.SiteReport;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * Customer-safe DTO for Site Reports.
 *
 * <p>Exposes the operational fields customers care about (weather,
 * manpower, equipment, work progress, distance to site) so the customer
 * app renders a real progress card instead of a bare title + description.
 * Internal-only audit fields stay hidden.
 */
@Getter
public class CustomerSiteReportDto {

    private Long id;
    private Long projectId;
    private String projectName;
    private String title;
    private String description;
    private String reportDate;
    private String status;
    private String reportType;
    private String submittedByName;

    private String weather;
    private Integer manpowerDeployed;
    private String equipmentUsed;
    private String workProgress;
    private Double latitude;
    private Double longitude;
    private Double distanceFromProject;

    private List<CustomerSiteReportPhotoDto> photos;
    /**
     * Per-activity breakdown (V84). Lets a single report show
     * "RCC slab pour: 8 men" alongside "Plastering: 4 men" instead of
     * a single rolled-up count. Empty for reports that predate the
     * activities table — populated by the controller via a separate
     * repo call to keep the customer SiteReport entity schema-light.
     */
    private List<CustomerSiteReportActivityDto> activities = Collections.emptyList();

    public CustomerSiteReportDto(SiteReport report) {
        this.id = report.getId();
        if (report.getProject() != null) {
            this.projectId = report.getProject().getId();
            this.projectName = report.getProject().getName();
        }
        this.title = report.getTitle();
        this.description = report.getDescription();
        this.reportDate = report.getReportDate() != null ? report.getReportDate().toString() : null;
        this.status = report.getStatus();
        this.reportType = report.getReportType();
        // Fall back to a generic label when the submitter has been deleted
        // or never set — the customer Flutter renders this directly; a raw
        // null collapses the card avatar/name fields.
        this.submittedByName = (report.getSubmittedByName() != null
                && !report.getSubmittedByName().isBlank())
                ? report.getSubmittedByName()
                : "Walldot staff";

        this.weather = report.getWeather();
        this.manpowerDeployed = report.getManpowerDeployed();
        this.equipmentUsed = report.getEquipmentUsed();
        this.workProgress = report.getWorkProgress();
        this.latitude = report.getLatitude();
        this.longitude = report.getLongitude();
        this.distanceFromProject = report.getDistanceFromProject();

        this.photos = report.getPhotos() != null
                ? report.getPhotos().stream()
                        .map(CustomerSiteReportPhotoDto::new)
                        .toList()
                : Collections.emptyList();
    }

    // Kept manual: setter has null-coalescing logic (not a plain assignment)
    public void setActivities(List<CustomerSiteReportActivityDto> activities) {
        this.activities = activities != null ? activities : Collections.emptyList();
    }
}
