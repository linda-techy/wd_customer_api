package com.wd.custapi.controller;

import com.wd.custapi.dto.SupportTicketReplyRequest;
import com.wd.custapi.dto.SupportTicketRequest;
import com.wd.custapi.service.SupportTicketService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support/tickets")
public class SupportTicketController {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketController.class);

    private static final String KEY_ERROR = "error";

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    private static ResponseEntity<Map<String, Object>> errorResponse(int status, String message) {
        return ResponseEntity.status(status).body(Map.of(KEY_ERROR, message));
    }

    /** An endpoint body that produces a success response or throws a runtime exception. */
    @FunctionalInterface
    private interface TicketAction {
        ResponseEntity<Map<String, Object>> run();
    }

    /**
     * Runs an endpoint body with the shared error contract: bad input → 400,
     * authorization failure → 403, anything else → 500 with {@code failureMessage}.
     * The error response bodies are identical to the previous per-endpoint handlers.
     */
    private ResponseEntity<Map<String, Object>> execute(String failureMessage, TicketAction action) {
        try {
            return action.run();
        } catch (IllegalArgumentException e) {
            logger.warn("{} (bad request): {}", failureMessage, e.getMessage());
            return errorResponse(400, e.getMessage());
        } catch (SecurityException e) {
            logger.warn("{} (access denied): {}", failureMessage, e.getMessage());
            return errorResponse(403, e.getMessage());
        } catch (Exception e) {
            logger.error(failureMessage, e);
            return errorResponse(500, failureMessage);
        }
    }

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> createTicket(@Valid @RequestBody SupportTicketRequest request,
                                          Authentication authentication) {
        return execute("Failed to create support ticket", () -> {
            String email = authentication.getName();
            Map<String, Object> ticket = supportTicketService.createTicket(email, request);
            return ResponseEntity.status(201).body(ticket);
        });
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getMyTickets(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String status,
                                          Authentication authentication) {
        return execute("Failed to list support tickets", () -> {
            String email = authentication.getName();
            Page<Map<String, Object>> tickets = supportTicketService.getMyTickets(email, status, page, size);
            return ResponseEntity.ok(Map.of(
                    "tickets", tickets.getContent(),
                    "totalElements", tickets.getTotalElements(),
                    "totalPages", tickets.getTotalPages(),
                    "page", tickets.getNumber(),
                    "size", tickets.getSize()
            ));
        });
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTicketDetail(@PathVariable Long id,
                                             Authentication authentication) {
        return execute("Failed to fetch ticket", () -> {
            String email = authentication.getName();
            Map<String, Object> ticket = supportTicketService.getTicketDetail(email, id);
            return ResponseEntity.ok(ticket);
        });
    }

    @PostMapping("/{id}/replies")
    public ResponseEntity<Map<String, Object>> addReply(@PathVariable Long id,
                                      @Valid @RequestBody SupportTicketReplyRequest request,
                                      Authentication authentication) {
        return execute("Failed to add reply", () -> {
            String email = authentication.getName();
            Map<String, Object> reply = supportTicketService.addReply(email, id, request);
            return ResponseEntity.status(201).body(reply);
        });
    }

    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<Map<String, Object>> getTicketsByProject(@PathVariable Long projectId,
                                                 Authentication authentication) {
        return execute("Failed to list tickets for project", () -> {
            String email = authentication.getName();
            List<?> tickets = supportTicketService.listByProjectForCustomer(projectId, email);
            return ResponseEntity.ok(Map.of("tickets", tickets));
        });
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<Map<String, Object>> closeTicket(@PathVariable Long id,
                                         Authentication authentication) {
        return execute("Failed to close ticket", () -> {
            String email = authentication.getName();
            Map<String, Object> ticket = supportTicketService.closeTicket(email, id);
            return ResponseEntity.ok(ticket);
        });
    }
}
