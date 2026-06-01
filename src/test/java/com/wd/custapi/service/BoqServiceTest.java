package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.*;
import com.wd.custapi.model.BoqCategory;
import com.wd.custapi.model.BoqItem;
import com.wd.custapi.model.BoqWorkType;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.BoqItemRepository;
import com.wd.custapi.repository.BoqWorkTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for {@link BoqService}. Both injected repositories are mocked.
 * BoqItem / Project / BoqWorkType / BoqCategory entities are read-only (no
 * setters) so they are stubbed as Mockito mocks where field values are needed.
 */
@ExtendWith(MockitoExtension.class)
class BoqServiceTest {

    @Mock
    private BoqItemRepository boqItemRepository;

    @Mock
    private BoqWorkTypeRepository boqWorkTypeRepository;

    @InjectMocks
    private BoqService boqService;

    // ----- helpers --------------------------------------------------------

    /** A fully-populated BoqItem mock with sensible defaults; tweak per test. */
    private BoqItem mockItem(Long id,
                             Long projectId,
                             BoqWorkType workType,
                             BoqCategory category,
                             BigDecimal amount,
                             BigDecimal executedAmount,
                             BigDecimal billedAmount,
                             String itemKind) {
        BoqItem item = mock(BoqItem.class);
        Project project = mock(Project.class);
        lenient().when(project.getId()).thenReturn(projectId);
        lenient().when(item.getId()).thenReturn(id);
        lenient().when(item.getProject()).thenReturn(project);
        lenient().when(item.getWorkType()).thenReturn(workType);
        lenient().when(item.getCategory()).thenReturn(category);
        lenient().when(item.getAmount()).thenReturn(amount);
        lenient().when(item.getTotalExecutedAmount()).thenReturn(executedAmount);
        lenient().when(item.getTotalBilledAmount()).thenReturn(billedAmount);
        lenient().when(item.getItemKind()).thenReturn(itemKind);
        // remaining mapped fields used by toDto
        lenient().when(item.getItemCode()).thenReturn("IC-" + id);
        lenient().when(item.getDescription()).thenReturn("desc-" + id);
        lenient().when(item.getQuantity()).thenReturn(BigDecimal.TEN);
        lenient().when(item.getUnit()).thenReturn("nos");
        lenient().when(item.getRate()).thenReturn(BigDecimal.ONE);
        lenient().when(item.getExecutedQuantity()).thenReturn(BigDecimal.ZERO);
        lenient().when(item.getBilledQuantity()).thenReturn(BigDecimal.ZERO);
        lenient().when(item.getRemainingQuantity()).thenReturn(BigDecimal.TEN);
        lenient().when(item.getExecutionPercentage()).thenReturn(BigDecimal.ZERO);
        lenient().when(item.getBillingPercentage()).thenReturn(BigDecimal.ZERO);
        lenient().when(item.getStatus()).thenReturn("APPROVED");
        lenient().when(item.getSpecifications()).thenReturn("spec");
        lenient().when(item.getNotes()).thenReturn("note");
        lenient().when(item.getCreatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(item.getUpdatedAt()).thenReturn(LocalDateTime.now());
        lenient().when(item.getCreatedByUserId()).thenReturn(99L);
        lenient().when(item.getIsActive()).thenReturn(true);
        return item;
    }

    private BoqWorkType workType(Long id, String name) {
        BoqWorkType wt = new BoqWorkType();
        wt.setId(id);
        wt.setName(name);
        wt.setDescription("d-" + name);
        wt.setDisplayOrder(1);
        return wt;
    }

    // ===== getProjectBoqItems ============================================

    @Test
    void getProjectBoqItems_withItems_mapsAllToDto() {
        BoqWorkType wt = workType(5L, "Civil");
        BoqCategory cat = mock(BoqCategory.class);
        when(cat.getId()).thenReturn(7L);
        when(cat.getName()).thenReturn("Foundation");

        BoqItem item = mockItem(1L, 100L, wt, cat,
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "BASE");

        when(boqItemRepository.findApprovedByProjectId(100L)).thenReturn(List.of(item));

        List<BoqItemDto> result = boqService.getProjectBoqItems(100L);

        assertEquals(1, result.size());
        BoqItemDto dto = result.get(0);
        assertEquals(1L, dto.id());
        assertEquals(100L, dto.projectId());
        assertEquals(5L, dto.workTypeId());
        assertEquals("Civil", dto.workTypeName());
        assertEquals(7L, dto.categoryId());
        assertEquals("Foundation", dto.categoryName());
        assertEquals("BASE", dto.itemKind());
        assertNull(dto.createdByName(), "createdByName is always null in customer API");
        verify(boqItemRepository).findApprovedByProjectId(100L);
    }

    @Test
    void getProjectBoqItems_nullWorkTypeAndCategory_mapsNullIds() {
        BoqItem item = mockItem(2L, 100L, null, null,
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "BASE");
        when(boqItemRepository.findApprovedByProjectId(100L)).thenReturn(List.of(item));

        List<BoqItemDto> result = boqService.getProjectBoqItems(100L);

        assertEquals(1, result.size());
        assertNull(result.get(0).workTypeId());
        assertNull(result.get(0).workTypeName());
        assertNull(result.get(0).categoryId());
        assertNull(result.get(0).categoryName());
    }

    @Test
    void getProjectBoqItems_empty_returnsEmptyList() {
        when(boqItemRepository.findApprovedByProjectId(100L)).thenReturn(List.of());
        assertTrue(boqService.getProjectBoqItems(100L).isEmpty());
    }

    // ===== getBoqItemsByWorkType =========================================

    @Test
    void getBoqItemsByWorkType_filtersByMatchingWorkType() {
        BoqWorkType wtA = workType(5L, "Civil");
        BoqWorkType wtB = workType(6L, "Electrical");
        BoqItem matching = mockItem(1L, 100L, wtA, null,
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "BASE");
        BoqItem other = mockItem(2L, 100L, wtB, null,
                new BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO, "BASE");

        when(boqItemRepository.findApprovedByProjectId(100L))
                .thenReturn(List.of(matching, other));

        List<BoqItemDto> result = boqService.getBoqItemsByWorkType(100L, 5L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(5L, result.get(0).workTypeId());
    }

    @Test
    void getBoqItemsByWorkType_nullWorkType_isExcluded() {
        BoqItem nullWt = mockItem(3L, 100L, null, null,
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "BASE");
        when(boqItemRepository.findApprovedByProjectId(100L)).thenReturn(List.of(nullWt));

        assertTrue(boqService.getBoqItemsByWorkType(100L, 5L).isEmpty());
    }

    @Test
    void getBoqItemsByWorkType_noMatch_returnsEmpty() {
        BoqWorkType wt = workType(6L, "Electrical");
        BoqItem item = mockItem(1L, 100L, wt, null,
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "BASE");
        when(boqItemRepository.findApprovedByProjectId(100L)).thenReturn(List.of(item));

        assertTrue(boqService.getBoqItemsByWorkType(100L, 5L).isEmpty());
    }

    // ===== getBoqSummary =================================================

    @Test
    void getBoqSummary_aggregatesAndComputesPercentages() {
        BoqWorkType wt = workType(5L, "Civil");
        // base item: amount 100, executed 50, billed 25
        BoqItem base = mockItem(1L, 100L, wt, null,
                new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("25"), "BASE");
        // addon item: amount 40, executed 10, billed 0
        BoqItem addon = mockItem(2L, 100L, wt, null,
                new BigDecimal("40"), new BigDecimal("10"), BigDecimal.ZERO, "ADDON");

        when(boqItemRepository.findByProjectIdWithAssociations(100L))
                .thenReturn(List.of(base, addon));

        BoqSummaryDto summary = boqService.getBoqSummary(100L);

        assertEquals(100L, summary.projectId());
        assertEquals(0, new BigDecimal("140").compareTo(summary.totalPlannedAmount()));
        assertEquals(0, new BigDecimal("60").compareTo(summary.totalExecutedAmount()));
        assertEquals(0, new BigDecimal("25").compareTo(summary.totalBilledAmount()));
        // executionPct = 60/140 * 100 = 42.86
        assertEquals(0, new BigDecimal("42.86").compareTo(summary.executionPercentage()));
        // billingPct = 25/60 * 100 = 41.67
        assertEquals(0, new BigDecimal("41.67").compareTo(summary.billingPercentage()));
        assertEquals(2, summary.totalItems());
        // baseScope = 100 (addon excluded), addon = 40
        assertEquals(0, new BigDecimal("100").compareTo(summary.baseScopeAmount()));
        assertEquals(0, new BigDecimal("40").compareTo(summary.addonAmount()));
        // both items share the same work type -> single summary group
        assertEquals(1, summary.workTypeSummaries().size());
        BoqWorkTypeSummary wts = summary.workTypeSummaries().get(0);
        assertEquals(5L, wts.workTypeId());
        assertEquals("Civil", wts.workTypeName());
        assertEquals(0, new BigDecimal("140").compareTo(wts.subtotal()));
        assertEquals(2, wts.itemCount());
    }

    @Test
    void getBoqSummary_empty_zeroPercentagesAndNoGroups() {
        when(boqItemRepository.findByProjectIdWithAssociations(100L)).thenReturn(List.of());

        BoqSummaryDto summary = boqService.getBoqSummary(100L);

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalPlannedAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalExecutedAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalBilledAmount()));
        // totalPlanned == 0 -> executionPct branch returns ZERO
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.executionPercentage()));
        // totalExecuted == 0 -> billingPct branch returns ZERO
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.billingPercentage()));
        assertEquals(0, summary.totalItems());
        assertTrue(summary.workTypeSummaries().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.baseScopeAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.addonAmount()));
    }

    @Test
    void getBoqSummary_nullWorkType_excludedFromGroupingButCountedInTotals() {
        BoqItem noWt = mockItem(1L, 100L, null, null,
                new BigDecimal("80"), BigDecimal.ZERO, BigDecimal.ZERO, "BASE");
        when(boqItemRepository.findByProjectIdWithAssociations(100L))
                .thenReturn(List.of(noWt));

        BoqSummaryDto summary = boqService.getBoqSummary(100L);

        assertEquals(1, summary.totalItems());
        assertTrue(summary.workTypeSummaries().isEmpty(),
                "null work-type item must not create a group");
        assertEquals(0, new BigDecimal("80").compareTo(summary.totalPlannedAmount()));
    }

    @Test
    void getBoqSummary_optionalAndExclusion_kindClassification() {
        BoqWorkType wt = workType(5L, "Civil");
        BoqItem optional = mockItem(1L, 100L, wt, null,
                new BigDecimal("30"), BigDecimal.ZERO, BigDecimal.ZERO, "OPTIONAL");
        BoqItem exclusion = mockItem(2L, 100L, wt, null,
                new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO, "EXCLUSION");

        when(boqItemRepository.findByProjectIdWithAssociations(100L))
                .thenReturn(List.of(optional, exclusion));

        BoqSummaryDto summary = boqService.getBoqSummary(100L);

        // OPTIONAL counts toward addon; EXCLUSION is neither base nor addon
        assertEquals(0, new BigDecimal("30").compareTo(summary.addonAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.baseScopeAmount()));
    }

    @Test
    void getBoqSummary_nullAmounts_areFilteredOut() {
        BoqWorkType wt = workType(5L, "Civil");
        BoqItem nullAmount = mockItem(1L, 100L, wt, null,
                null, BigDecimal.ZERO, BigDecimal.ZERO, "BASE");
        when(boqItemRepository.findByProjectIdWithAssociations(100L))
                .thenReturn(List.of(nullAmount));

        BoqSummaryDto summary = boqService.getBoqSummary(100L);

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalPlannedAmount()));
        assertEquals(1, summary.totalItems());
        // grouped subtotal with null amount filtered -> ZERO
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.workTypeSummaries().get(0).subtotal()));
    }

    // ===== getAllWorkTypes ===============================================

    @Test
    void getAllWorkTypes_mapsToDto() {
        when(boqWorkTypeRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(workType(5L, "Civil"), workType(6L, "Electrical")));

        List<BoqWorkTypeDto> result = boqService.getAllWorkTypes();

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).id());
        assertEquals("Civil", result.get(0).name());
        assertEquals("d-Civil", result.get(0).description());
        assertEquals(1, result.get(0).displayOrder());
        assertEquals("Electrical", result.get(1).name());
    }

    @Test
    void getAllWorkTypes_empty_returnsEmpty() {
        when(boqWorkTypeRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());
        assertTrue(boqService.getAllWorkTypes().isEmpty());
    }
}
