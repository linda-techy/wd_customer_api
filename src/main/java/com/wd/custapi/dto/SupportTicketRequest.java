package com.wd.custapi.dto;

import jakarta.validation.constraints.NotBlank;

public class SupportTicketRequest {

    @NotBlank
    private String subject;

    @NotBlank
    private String description;

    private String category = "GENERAL";

    private String priority = "MEDIUM";

    private Long projectId;

    // Getters and Setters

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
}
