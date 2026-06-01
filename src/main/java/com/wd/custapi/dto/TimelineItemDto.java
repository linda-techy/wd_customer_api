package com.wd.custapi.dto;

import java.time.LocalDate;

public class TimelineItemDto {
    private Long taskId;
    private String title;
    private String milestoneName;
    private Long milestoneId;
    private LocalDate plannedStart;
    private LocalDate plannedEnd;
    private LocalDate actualStart;
    private LocalDate actualEnd;
    private Integer progressPercent;
    private String status;
    private String statusLabel;
    private String crewName;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMilestoneName() {
        return milestoneName;
    }

    public void setMilestoneName(String milestoneName) {
        this.milestoneName = milestoneName;
    }

    public Long getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Long milestoneId) {
        this.milestoneId = milestoneId;
    }

    public LocalDate getPlannedStart() {
        return plannedStart;
    }

    public void setPlannedStart(LocalDate plannedStart) {
        this.plannedStart = plannedStart;
    }

    public LocalDate getPlannedEnd() {
        return plannedEnd;
    }

    public void setPlannedEnd(LocalDate plannedEnd) {
        this.plannedEnd = plannedEnd;
    }

    public LocalDate getActualStart() {
        return actualStart;
    }

    public void setActualStart(LocalDate actualStart) {
        this.actualStart = actualStart;
    }

    public LocalDate getActualEnd() {
        return actualEnd;
    }

    public void setActualEnd(LocalDate actualEnd) {
        this.actualEnd = actualEnd;
    }

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getCrewName() {
        return crewName;
    }

    public void setCrewName(String crewName) {
        this.crewName = crewName;
    }
}
