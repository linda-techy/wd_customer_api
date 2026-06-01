package com.wd.custapi.dto;

public class TimelineSummaryDto {
    private Integer weekCount;
    private Integer upcomingCount;
    private Integer completedCount;
    private Integer projectProgressPercent;

    public Integer getWeekCount() {
        return weekCount;
    }

    public void setWeekCount(Integer weekCount) {
        this.weekCount = weekCount;
    }

    public Integer getUpcomingCount() {
        return upcomingCount;
    }

    public void setUpcomingCount(Integer upcomingCount) {
        this.upcomingCount = upcomingCount;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Integer completedCount) {
        this.completedCount = completedCount;
    }

    public Integer getProjectProgressPercent() {
        return projectProgressPercent;
    }

    public void setProjectProgressPercent(Integer projectProgressPercent) {
        this.projectProgressPercent = projectProgressPercent;
    }
}
