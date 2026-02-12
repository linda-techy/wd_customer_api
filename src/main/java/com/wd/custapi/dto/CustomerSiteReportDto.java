package com.wd.custapi.dto;

import com.wd.custapi.model.SiteReport;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Customer-safe DTO for Site Reports.
 * Excludes sensitive company information like internal notes, employee details.
 */
public class CustomerSiteReportDto {

    private Long id;
    private Long projectId;
    private String projectName;
    private String title;
    private String description;
    private String reportDate;
    private String status;
    private String reportType;
    private List<CustomerSiteReportPhotoDto> photos;

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

        if (report.getPhotos() != null) {
            this.photos = report.getPhotos().stream()
                    .map(CustomerSiteReportPhotoDto::new)
                    .collect(Collectors.toList());
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public List<CustomerSiteReportPhotoDto> getPhotos() { return photos; }
    public void setPhotos(List<CustomerSiteReportPhotoDto> photos) { this.photos = photos; }
}
