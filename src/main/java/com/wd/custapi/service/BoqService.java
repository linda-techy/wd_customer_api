package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.*;
import com.wd.custapi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BoqService {
    
    private final BoqItemRepository boqItemRepository;
    private final BoqWorkTypeRepository boqWorkTypeRepository;
    private final ProjectRepository projectRepository;
    private final CustomerUserRepository userRepository;
    private final ActivityFeedService activityFeedService;
    
    public BoqService(BoqItemRepository boqItemRepository,
                      BoqWorkTypeRepository boqWorkTypeRepository,
                      ProjectRepository projectRepository,
                      CustomerUserRepository userRepository,
                      ActivityFeedService activityFeedService) {
        this.boqItemRepository = boqItemRepository;
        this.boqWorkTypeRepository = boqWorkTypeRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.activityFeedService = activityFeedService;
    }
    
    @Transactional
    public BoqItemDto addBoqItem(Long projectId, BoqItemRequest request, Long userId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        
        BoqWorkType workType = boqWorkTypeRepository.findById(request.workTypeId())
            .orElseThrow(() -> new RuntimeException("Work type not found"));
        
        CustomerUser user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        BoqItem item = new BoqItem();
        item.setProject(project);
        item.setWorkType(workType);
        item.setItemCode(request.itemCode());
        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnit(request.unit());
        item.setRate(request.rate());
        item.setSpecifications(request.specifications());
        item.setNotes(request.notes());
        item.setCreatedBy(user);
        
        item = boqItemRepository.save(item);
        
        // Create activity feed
        activityFeedService.createActivity(projectId, "BOQ_UPDATED", 
            "BoQ item added: " + request.description(), item.getId(), userId);
        
        return toDto(item);
    }
    
    @Transactional
    public BoqItemDto updateBoqItem(Long itemId, BoqItemRequest request, Long userId) {
        BoqItem item = boqItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("BoQ item not found"));
        
        item.setItemCode(request.itemCode());
        item.setDescription(request.description());
        item.setQuantity(request.quantity());
        item.setUnit(request.unit());
        item.setRate(request.rate());
        item.setSpecifications(request.specifications());
        item.setNotes(request.notes());
        
        if (request.workTypeId() != null) {
            BoqWorkType workType = boqWorkTypeRepository.findById(request.workTypeId())
                .orElseThrow(() -> new RuntimeException("Work type not found"));
            item.setWorkType(workType);
        }
        
        item = boqItemRepository.save(item);
        
        // Create activity feed
        activityFeedService.createActivity(item.getProject().getId(), "BOQ_UPDATED", 
            "BoQ item updated: " + request.description(), item.getId(), userId);
        
        return toDto(item);
    }
    
    public List<BoqItemDto> getProjectBoqItems(Long projectId) {
        return boqItemRepository.findByProjectIdAndIsActiveTrue(projectId)
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
        List<BoqItem> items = boqItemRepository.findByProjectIdAndIsActiveTrue(projectId);
        
        BigDecimal totalAmount = boqItemRepository.getTotalAmountByProjectId(projectId);
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        
        Map<BoqWorkType, List<BoqItem>> groupedItems = items.stream()
            .collect(Collectors.groupingBy(BoqItem::getWorkType));
        
        List<BoqWorkTypeSummary> workTypeSummaries = groupedItems.entrySet().stream()
            .map(entry -> {
                BigDecimal subtotal = entry.getValue().stream()
                    .map(item -> item.getQuantity().multiply(item.getRate()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                return new BoqWorkTypeSummary(
                    entry.getKey().getId(),
                    entry.getKey().getName(),
                    subtotal,
                    entry.getValue().size()
                );
            })
            .collect(Collectors.toList());
        
        return new BoqSummaryDto(projectId, totalAmount, workTypeSummaries);
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
        return new BoqItemDto(
            item.getId(),
            item.getProject().getId(),
            item.getWorkType().getId(),
            item.getWorkType().getName(),
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
            item.getCreatedBy().getId(),
            item.getCreatedBy().getFirstName() + " " + item.getCreatedBy().getLastName(),
            item.getIsActive()
        );
    }
}

