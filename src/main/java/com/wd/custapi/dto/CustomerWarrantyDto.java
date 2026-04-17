package com.wd.custapi.dto;

import com.wd.custapi.model.ProjectWarranty;
import com.wd.custapi.model.enums.WarrantyStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CustomerWarrantyDto(
        Long id,
        String componentName,
        String description,
        String providerName,
        LocalDate startDate,
        LocalDate endDate,
        WarrantyStatus status,
        String coverageDetails,
        LocalDateTime createdAt
) {
    public static CustomerWarrantyDto from(ProjectWarranty w) {
        return new CustomerWarrantyDto(
                w.getId(),
                w.getComponentName(),
                w.getDescription(),
                w.getProviderName(),
                w.getStartDate(),
                w.getEndDate(),
                w.getStatus(),
                w.getCoverageDetails(),
                w.getCreatedAt()
        );
    }
}
