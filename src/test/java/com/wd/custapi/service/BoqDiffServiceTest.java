package com.wd.custapi.service;

import com.wd.custapi.model.BoqDocument;
import com.wd.custapi.model.BoqItem;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.enums.BoqDocumentStatus;
import com.wd.custapi.repository.BoqDocumentRepository;
import com.wd.custapi.repository.BoqItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoqDiffServiceTest {

    @Mock
    private BoqDocumentRepository boqDocumentRepository;

    @Mock
    private BoqItemRepository boqItemRepository;

    @InjectMocks
    private BoqDiffService boqDiffService;

    private Project project;
    private BoqDocument fromDoc;
    private BoqDocument toDoc;

    @BeforeEach
    void setUp() {
        project = new Project();
        ReflectionTestUtils.setField(project, "id", 10L);

        fromDoc = new BoqDocument();
        ReflectionTestUtils.setField(fromDoc, "id", 1L);
        ReflectionTestUtils.setField(fromDoc, "project", project);
        ReflectionTestUtils.setField(fromDoc, "revisionNumber", 1);
        ReflectionTestUtils.setField(fromDoc, "status", BoqDocumentStatus.APPROVED);

        toDoc = new BoqDocument();
        ReflectionTestUtils.setField(toDoc, "id", 2L);
        ReflectionTestUtils.setField(toDoc, "project", project);
        ReflectionTestUtils.setField(toDoc, "revisionNumber", 2);
        ReflectionTestUtils.setField(toDoc, "status", BoqDocumentStatus.APPROVED);
    }

    // ── getDiff: added items ──────────────────────────────────────────────────

    @Test
    void getDiff_itemAddedInNewRevision_appearsInAdded() {
        when(boqDocumentRepository.findById(1L)).thenReturn(Optional.of(fromDoc));
        when(boqDocumentRepository.findById(2L)).thenReturn(Optional.of(toDoc));

        BoqItem existingItem = makeItem("ITEM-001", "Foundation work", bd("100"), bd("500"));
        BoqItem newItem     = makeItem("ITEM-002", "Roofing",          bd("50"),  bd("800"));

        when(boqItemRepository.findByBoqDocumentId(1L)).thenReturn(List.of(existingItem));
        when(boqItemRepository.findByBoqDocumentId(2L)).thenReturn(List.of(existingItem, newItem));

        Map<String, Object> diff = boqDiffService.getDiff(10L, 1L, 2L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> added = (List<Map<String, Object>>) diff.get("added");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> removed = (List<Map<String, Object>>) diff.get("removed");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modified = (List<Map<String, Object>>) diff.get("modified");

        assertEquals(1, added.size());
        assertEquals("ITEM-002", added.get(0).get("itemCode"));
        assertEquals(0, removed.size());
        assertEquals(0, modified.size());
    }

    // ── getDiff: removed items ────────────────────────────────────────────────

    @Test
    void getDiff_itemRemovedInNewRevision_appearsInRemoved() {
        when(boqDocumentRepository.findById(1L)).thenReturn(Optional.of(fromDoc));
        when(boqDocumentRepository.findById(2L)).thenReturn(Optional.of(toDoc));

        BoqItem item1 = makeItem("ITEM-001", "Foundation", bd("100"), bd("500"));
        BoqItem item2 = makeItem("ITEM-002", "Painting",   bd("200"), bd("50"));

        when(boqItemRepository.findByBoqDocumentId(1L)).thenReturn(List.of(item1, item2));
        when(boqItemRepository.findByBoqDocumentId(2L)).thenReturn(List.of(item1));

        Map<String, Object> diff = boqDiffService.getDiff(10L, 1L, 2L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> removed = (List<Map<String, Object>>) diff.get("removed");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> added = (List<Map<String, Object>>) diff.get("added");

        assertEquals(1, removed.size());
        assertEquals("ITEM-002", removed.get(0).get("itemCode"));
        assertEquals(0, added.size());
    }

    // ── getDiff: modified items ───────────────────────────────────────────────

    @Test
    void getDiff_itemQuantityChanged_appearsInModified() {
        when(boqDocumentRepository.findById(1L)).thenReturn(Optional.of(fromDoc));
        when(boqDocumentRepository.findById(2L)).thenReturn(Optional.of(toDoc));

        BoqItem fromItem = makeItem("ITEM-001", "Foundation", bd("100"), bd("500"));
        BoqItem toItem   = makeItem("ITEM-001", "Foundation", bd("120"), bd("500")); // qty changed

        when(boqItemRepository.findByBoqDocumentId(1L)).thenReturn(List.of(fromItem));
        when(boqItemRepository.findByBoqDocumentId(2L)).thenReturn(List.of(toItem));

        Map<String, Object> diff = boqDiffService.getDiff(10L, 1L, 2L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modified = (List<Map<String, Object>>) diff.get("modified");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> added = (List<Map<String, Object>>) diff.get("added");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> removed = (List<Map<String, Object>>) diff.get("removed");

        assertEquals(1, modified.size());
        assertEquals("ITEM-001", modified.get(0).get("itemCode"));
        assertEquals(0, added.size());
        assertEquals(0, removed.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> changes = (Map<String, Object>) modified.get(0).get("changes");
        assertTrue(changes.containsKey("quantity"));
    }

    // ── getDiff: summary totals ───────────────────────────────────────────────

    @Test
    void getDiff_summary_reflectsCorrectTotalsAndRevisions() {
        when(boqDocumentRepository.findById(1L)).thenReturn(Optional.of(fromDoc));
        when(boqDocumentRepository.findById(2L)).thenReturn(Optional.of(toDoc));

        // fromDoc total: 100 * 500 = 50000
        BoqItem fromItem = makeItem("ITEM-001", "Foundation", bd("100"), bd("500"));
        // toDoc total: 100 * 600 = 60000
        BoqItem toItem   = makeItem("ITEM-001", "Foundation", bd("100"), bd("600"));

        when(boqItemRepository.findByBoqDocumentId(1L)).thenReturn(List.of(fromItem));
        when(boqItemRepository.findByBoqDocumentId(2L)).thenReturn(List.of(toItem));

        Map<String, Object> diff = boqDiffService.getDiff(10L, 1L, 2L);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) diff.get("summary");

        assertEquals(1, summary.get("fromRevision"));
        assertEquals(2, summary.get("toRevision"));
        assertTrue(((BigDecimal) summary.get("delta")).compareTo(BigDecimal.ZERO) > 0,
                "delta should be positive when rates increased");
    }

    // ── getDiff: wrong project ownership ────────────────────────────────────

    @Test
    void getDiff_documentBelongsToDifferentProject_throwsIllegalArgumentException() {
        Project otherProject = new Project();
        ReflectionTestUtils.setField(otherProject, "id", 99L);

        BoqDocument wrongDoc = new BoqDocument();
        ReflectionTestUtils.setField(wrongDoc, "id", 1L);
        ReflectionTestUtils.setField(wrongDoc, "project", otherProject);

        when(boqDocumentRepository.findById(1L)).thenReturn(Optional.of(wrongDoc));
        when(boqDocumentRepository.findById(2L)).thenReturn(Optional.of(toDoc));

        assertThrows(IllegalArgumentException.class,
                () -> boqDiffService.getDiff(10L, 1L, 2L));
    }

    // ── getDiff: no changes ───────────────────────────────────────────────────

    @Test
    void getDiff_identicalRevisions_returnsEmptyDiff() {
        when(boqDocumentRepository.findById(1L)).thenReturn(Optional.of(fromDoc));
        when(boqDocumentRepository.findById(2L)).thenReturn(Optional.of(toDoc));

        BoqItem item = makeItem("ITEM-001", "Foundation", bd("100"), bd("500"));

        when(boqItemRepository.findByBoqDocumentId(1L)).thenReturn(List.of(item));
        when(boqItemRepository.findByBoqDocumentId(2L)).thenReturn(List.of(item));

        Map<String, Object> diff = boqDiffService.getDiff(10L, 1L, 2L);

        @SuppressWarnings("unchecked")
        List<?> added    = (List<?>) diff.get("added");
        @SuppressWarnings("unchecked")
        List<?> removed  = (List<?>) diff.get("removed");
        @SuppressWarnings("unchecked")
        List<?> modified = (List<?>) diff.get("modified");

        assertTrue(added.isEmpty());
        assertTrue(removed.isEmpty());
        assertTrue(modified.isEmpty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BoqItem makeItem(String code, String description, BigDecimal qty, BigDecimal rate) {
        BoqItem item = new BoqItem();
        ReflectionTestUtils.setField(item, "id", (long) code.hashCode());
        ReflectionTestUtils.setField(item, "itemCode", code);
        ReflectionTestUtils.setField(item, "description", description);
        ReflectionTestUtils.setField(item, "quantity", qty);
        ReflectionTestUtils.setField(item, "rate", rate);
        ReflectionTestUtils.setField(item, "amount", qty.multiply(rate));
        return item;
    }

    private BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
