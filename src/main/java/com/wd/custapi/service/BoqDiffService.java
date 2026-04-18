package com.wd.custapi.service;

import com.wd.custapi.model.BoqDocument;
import com.wd.custapi.model.BoqItem;
import com.wd.custapi.repository.BoqDocumentRepository;
import com.wd.custapi.repository.BoqItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computes side-by-side diff between two BOQ document revisions.
 * All reads are from portal-owned tables; this service is read-only.
 */
@Service
public class BoqDiffService {

    private final BoqDocumentRepository boqDocumentRepository;
    private final BoqItemRepository boqItemRepository;

    public BoqDiffService(BoqDocumentRepository boqDocumentRepository,
                          BoqItemRepository boqItemRepository) {
        this.boqDocumentRepository = boqDocumentRepository;
        this.boqItemRepository = boqItemRepository;
    }

    /**
     * Returns all BOQ document revisions for a project, ordered oldest first.
     */
    public List<Map<String, Object>> getRevisions(Long projectId) {
        return boqDocumentRepository.findByProjectIdOrderByRevisionNumberAsc(projectId)
                .stream()
                .map(this::revisionToMap)
                .collect(Collectors.toList());
    }

    /**
     * Computes a structured diff between two BOQ document snapshots.
     *
     * @param projectId  owning project (used to verify document ownership)
     * @param fromDocId  older revision document id
     * @param toDocId    newer revision document id
     * @return map with keys: added, removed, modified, summary
     * @throws IllegalArgumentException if either document is not found or does not
     *                                  belong to the given project
     */
    public Map<String, Object> getDiff(Long projectId, Long fromDocId, Long toDocId) {
        BoqDocument fromDoc = boqDocumentRepository.findById(fromDocId)
                .orElseThrow(() -> new IllegalArgumentException("From-document not found: " + fromDocId));
        BoqDocument toDoc = boqDocumentRepository.findById(toDocId)
                .orElseThrow(() -> new IllegalArgumentException("To-document not found: " + toDocId));

        if (!fromDoc.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("From-document does not belong to project " + projectId);
        }
        if (!toDoc.getProject().getId().equals(projectId)) {
            throw new IllegalArgumentException("To-document does not belong to project " + projectId);
        }

        List<BoqItem> fromItems = boqItemRepository.findByBoqDocumentId(fromDocId);
        List<BoqItem> toItems = boqItemRepository.findByBoqDocumentId(toDocId);

        // Index by item_code (treat null item_code as the item's id.toString() as fallback)
        Map<String, BoqItem> fromMap = index(fromItems);
        Map<String, BoqItem> toMap = index(toItems);

        List<Map<String, Object>> added = new ArrayList<>();
        List<Map<String, Object>> removed = new ArrayList<>();
        List<Map<String, Object>> modified = new ArrayList<>();

        // Added: in toDoc but not in fromDoc
        for (Map.Entry<String, BoqItem> entry : toMap.entrySet()) {
            if (!fromMap.containsKey(entry.getKey())) {
                added.add(itemToMap(entry.getValue()));
            }
        }

        // Removed: in fromDoc but not in toDoc
        for (Map.Entry<String, BoqItem> entry : fromMap.entrySet()) {
            if (!toMap.containsKey(entry.getKey())) {
                removed.add(itemToMap(entry.getValue()));
            }
        }

        // Modified: in both — check quantity, rate, description
        for (Map.Entry<String, BoqItem> entry : fromMap.entrySet()) {
            if (!toMap.containsKey(entry.getKey())) continue;
            BoqItem from = entry.getValue();
            BoqItem to = toMap.get(entry.getKey());
            Map<String, Object> changes = buildChanges(from, to);
            if (!changes.isEmpty()) {
                Map<String, Object> modItem = new LinkedHashMap<>();
                modItem.put("itemCode", entry.getKey());
                modItem.put("description", to.getDescription());
                modItem.put("changes", changes);
                modified.add(modItem);
            }
        }

        // Summary
        BigDecimal oldTotal = sumAmounts(fromItems);
        BigDecimal newTotal = sumAmounts(toItems);
        BigDecimal delta = newTotal.subtract(oldTotal);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("oldTotal", oldTotal);
        summary.put("newTotal", newTotal);
        summary.put("delta", delta);
        summary.put("addedCount", added.size());
        summary.put("removedCount", removed.size());
        summary.put("modifiedCount", modified.size());
        summary.put("fromRevision", fromDoc.getRevisionNumber());
        summary.put("toRevision", toDoc.getRevisionNumber());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("added", added);
        result.put("removed", removed);
        result.put("modified", modified);
        result.put("summary", summary);
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, BoqItem> index(List<BoqItem> items) {
        Map<String, BoqItem> map = new LinkedHashMap<>();
        for (BoqItem item : items) {
            String key = item.getItemCode() != null && !item.getItemCode().isBlank()
                    ? item.getItemCode()
                    : "id:" + item.getId();
            map.put(key, item);
        }
        return map;
    }

    private Map<String, Object> buildChanges(BoqItem from, BoqItem to) {
        Map<String, Object> changes = new LinkedHashMap<>();

        if (!Objects.equals(trim(from.getDescription()), trim(to.getDescription()))) {
            changes.put("description", changeMap(from.getDescription(), to.getDescription()));
        }
        if (notEqual(from.getQuantity(), to.getQuantity())) {
            changes.put("quantity", changeMap(from.getQuantity(), to.getQuantity()));
        }
        if (notEqual(from.getRate(), to.getRate())) {
            changes.put("rate", changeMap(from.getRate(), to.getRate()));
        }
        return changes;
    }

    private Map<String, Object> changeMap(Object oldVal, Object newVal) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("oldValue", oldVal);
        m.put("newValue", newVal);
        return m;
    }

    private boolean notEqual(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return false;
        if (a == null || b == null) return true;
        return a.compareTo(b) != 0;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private BigDecimal sumAmounts(List<BoqItem> items) {
        return items.stream()
                .map(BoqItem::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> revisionToMap(BoqDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("revisionNumber", d.getRevisionNumber());
        m.put("status", d.getStatus());
        m.put("createdAt", d.getCreatedAt());
        m.put("totalValueExGst", d.getTotalValueExGst());
        m.put("totalValueInclGst", d.getTotalValueInclGst());
        return m;
    }

    private Map<String, Object> itemToMap(BoqItem item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("itemCode", item.getItemCode());
        m.put("description", item.getDescription());
        m.put("quantity", item.getQuantity());
        m.put("unit", item.getUnit());
        m.put("rate", item.getRate());
        m.put("amount", item.getAmount());
        return m;
    }
}
