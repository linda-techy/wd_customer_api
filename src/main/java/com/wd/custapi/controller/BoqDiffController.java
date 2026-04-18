package com.wd.custapi.controller;

import com.wd.custapi.model.Project;
import com.wd.custapi.service.BoqDiffService;
import com.wd.custapi.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Exposes BOQ revision comparison endpoints to authenticated customers.
 *
 * <pre>
 *   GET /api/projects/{projectId}/boq/revisions
 *   GET /api/projects/{projectId}/boq/diff?fromDoc={id}&amp;toDoc={id}
 * </pre>
 *
 * Both endpoints require the caller to be authenticated and to have project
 * access (enforced via {@link DashboardService#getProjectByUuidAndEmail}).
 */
@RestController
@RequestMapping("/api/projects/{projectId}/boq")
@PreAuthorize("isAuthenticated()")
public class BoqDiffController {

    private static final Logger logger = LoggerFactory.getLogger(BoqDiffController.class);

    private final DashboardService dashboardService;
    private final BoqDiffService boqDiffService;

    public BoqDiffController(DashboardService dashboardService,
                              BoqDiffService boqDiffService) {
        this.dashboardService = dashboardService;
        this.boqDiffService = boqDiffService;
    }

    /**
     * Returns the list of BOQ document revisions for the project (oldest first).
     * Each entry includes: id, revisionNumber, status, createdAt, totals.
     */
    @GetMapping("/revisions")
    public ResponseEntity<Map<String, Object>> getRevisions(
            @PathVariable("projectId") String projectUuid,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            List<Map<String, Object>> revisions = boqDiffService.getRevisions(project.getId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "revisions", revisions,
                    "count", revisions.size()
            ));
        } catch (Exception e) {
            logger.error("Failed to fetch BOQ revisions for project {}", projectUuid, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }

    /**
     * Computes a side-by-side diff between two BOQ document snapshots.
     *
     * @param fromDoc  document id of the older revision
     * @param toDoc    document id of the newer revision
     * @return structured diff with added, removed, modified lists and a summary
     */
    @GetMapping("/diff")
    public ResponseEntity<Map<String, Object>> getDiff(
            @PathVariable("projectId") String projectUuid,
            @RequestParam("fromDoc") Long fromDocId,
            @RequestParam("toDoc") Long toDocId,
            Authentication auth) {
        try {
            String email = auth.getName();
            Project project = dashboardService.getProjectByUuidAndEmail(projectUuid, email);
            Map<String, Object> diff = boqDiffService.getDiff(project.getId(), fromDocId, toDocId);
            return ResponseEntity.ok(Map.of("success", true, "data", diff));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(
                    Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to compute BOQ diff for project {} ({} vs {})",
                    projectUuid, fromDocId, toDocId, e);
            return ResponseEntity.status(500).body(
                    Map.of("success", false, "message", "An internal error occurred"));
        }
    }
}
