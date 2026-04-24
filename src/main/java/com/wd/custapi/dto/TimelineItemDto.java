package com.wd.custapi.dto;

import java.time.LocalDate;

public class TimelineItemDto {
    public Long taskId;
    public String title;
    public String milestoneName;
    public Long milestoneId;
    public LocalDate plannedStart;
    public LocalDate plannedEnd;
    public LocalDate actualStart;
    public LocalDate actualEnd;
    public Integer progressPercent;
    public String status;
    public String statusLabel;
    public String crewName;
}
