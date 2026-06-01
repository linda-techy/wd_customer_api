package com.wd.custapi.dto;

import java.util.List;

public class TimelineResponseDto {
    private String bucket;
    private List<TimelineItemDto> items;
    private Long totalElements;
    private Integer totalPages;
    private Integer page;
    private Integer size;
    private Integer projectProgressPercent;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public List<TimelineItemDto> getItems() {
        return items;
    }

    public void setItems(List<TimelineItemDto> items) {
        this.items = items;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Long totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getProjectProgressPercent() {
        return projectProgressPercent;
    }

    public void setProjectProgressPercent(Integer projectProgressPercent) {
        this.projectProgressPercent = projectProgressPercent;
    }
}
