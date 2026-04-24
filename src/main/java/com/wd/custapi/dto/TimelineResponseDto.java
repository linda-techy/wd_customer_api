package com.wd.custapi.dto;

import java.util.List;

public class TimelineResponseDto {
    public String bucket;
    public List<TimelineItemDto> items;
    public Long totalElements;
    public Integer totalPages;
    public Integer page;
    public Integer size;
    public Integer projectProgressPercent;
}
