package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BoqService {
    
    private final BoqItemRepository boqItemRepository;
    private final BoqWorkTypeRepository boqWorkTypeRepository;
    
    public BoqService(BoqItemRepository boqItemRepository,
                      BoqWorkTypeRepository boqWorkTypeRepository) {
        this.boqItemRepository = boqItemRepository;
        this.boqWorkTypeRepository = boqWorkTypeRepository;
    }
    
    public List<BoqItemDto> getProjectBoqItems(Long projectId) {
        return boqItemRepository.findByProjectIdWithAssociations(projectId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    public List<BoqItemDto> getBoqItemsByWorkType(Long projectId, Long workTypeId) {
        return boqItemRepository.findByProjectIdAndWorkTypeIdAndIsActiveTrue(projectId, workTypeId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    public BoqSummaryDto getBoqSummary(Long projectId) {
        List<BoqItem> items = boqItemRepository.findByProjectIdWithAssociations(projectId);

        BigDecimal totalPlanned = items.stream()
            .map(BoqItem::getAmount)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExecuted = items.stream()
            .map(BoqItem::getTotalExecutedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalBilled = items.stream()
            .map(BoqItem::getTotalBilledAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal executionPct = totalPlanned.compareTo(BigDecimal.ZERO) > 0
            ? totalExecuted.divide(totalPlanned, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        BigDecimal billingPct = totalExecuted.compareTo(BigDecimal.ZERO) > 0
            ? totalBilled.divide(totalExecuted, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        // Guard against null workType (Portal API allows items without a work type)
        Map<BoqWorkType, List<BoqItem>> groupedItems = items.stream()
            .filter(i -> i.getWorkType() != null)
            .collect(Collectors.groupingBy(BoqItem::getWorkType));

        List<BoqWorkTypeSummary> workTypeSummaries = groupedItems.entrySet().stream()
            .map(entry -> {
                BigDecimal subtotal = entry.getValue().stream()
                    .map(BoqItem::getAmount)
                    .filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new BoqWorkTypeSummary(
                    entry.getKey().getId(),
                    entry.getKey().getName(),
                    subtotal,
                    entry.getValue().size()
                );
            })
            .collect(Collectors.toList());

        return new BoqSummaryDto(
            projectId,
            totalPlanned,
            totalExecuted,
            totalBilled,
            executionPct,
            billingPct,
            items.size(),
            workTypeSummaries
        );
    }
    
    public List<BoqWorkTypeDto> getAllWorkTypes() {
        return boqWorkTypeRepository.findAllByOrderByDisplayOrderAsc()
            .stream()
            .map(wt -> new BoqWorkTypeDto(wt.getId(), wt.getName(), wt.getDescription(), wt.getDisplayOrder()))
            .collect(Collectors.toList());
    }
    
    private BoqItemDto toDto(BoqItem item) {
        Long categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        String categoryName = item.getCategory() != null ? item.getCategory().getName() : null;
        // workType is nullable (Portal API allows it); guard defensively
        Long workTypeId = item.getWorkType() != null ? item.getWorkType().getId() : null;
        String workTypeName = item.getWorkType() != null ? item.getWorkType().getName() : null;
        return new BoqItemDto(
            item.getId(),
            item.getProject().getId(),
            workTypeId,
            workTypeName,
            categoryId,
            categoryName,
            item.getItemCode(),
            item.getDescription(),
            item.getQuantity(),
            item.getUnit(),
            item.getRate(),
            item.getAmount(),
            item.getExecutedQuantity(),
            item.getBilledQuantity(),
            item.getRemainingQuantity(),
            item.getTotalExecutedAmount(),
            item.getTotalBilledAmount(),
            item.getExecutionPercentage(),
            item.getBillingPercentage(),
            item.getStatus(),
            item.getSpecifications(),
            item.getNotes(),
            item.getCreatedAt(),
            item.getUpdatedAt(),
            item.getCreatedByUserId(),
            null,  // createdByName not available in customer API (Portal user)
            item.getIsActive()
        );
    }
}

