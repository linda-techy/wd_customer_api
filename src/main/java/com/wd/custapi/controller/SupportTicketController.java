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

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> createTicket(@Valid @RequestBody SupportTicketRequest request,
                                          Authentication authentication) {
        try {
            String email = authentication.getName();
            Map<String, Object> ticket = supportTicketService.createTicket(email, request);
            return ResponseEntity.status(201).body(ticket);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request creating support ticket: {}", e.getMessage());
            return errorResponse(400, e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating support ticket", e);
            return errorResponse(500, "Failed to create support ticket");
        }
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getMyTickets(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String status,
                                          Authentication authentication) {
        try {
            String email = authentication.getName();
            Page<Map<String, Object>> tickets = supportTicketService.getMyTickets(email, status, page, size);
            return ResponseEntity.ok(Map.of(
                    "tickets", tickets.getContent(),
                    "totalElements", tickets.getTotalElements(),
                    "totalPages", tickets.getTotalPages(),
                    "page", tickets.getNumber(),
                    "size", tickets.getSize()
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request listing support tickets: {}", e.getMessage());
            return errorResponse(400, e.getMessage());
        } catch (Exception e) {
            logger.error("Error listing support tickets", e);
            return errorResponse(500, "Failed to list support tickets");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getTicketDetail(@PathVariable Long id,
                                             Authentication authentication) {
        try {
            String email = authentication.getName();
            Map<String, Object> ticket = supportTicketService.getTicketDetail(email, id);
            return ResponseEntity.ok(ticket);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request fetching ticket {}: {}", id, e.getMessage());
            return errorResponse(400, e.getMessage());
        } catch (SecurityException e) {
            logger.warn("Access denied for ticket {}: {}", id, e.getMessage());
            return errorResponse(403, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching ticket {}", id, e);
            return errorResponse(500, "Failed to fetch ticket");
        }
    }

    @PostMapping("/{id}/replies")
    public ResponseEntity<Map<String, Object>> addReply(@PathVariable Long id,
                                      @Valid @RequestBody SupportTicketReplyRequest request,
                                      Authentication authentication) {
        try {
            String email = authentication.getName();
            Map<String, Object> reply = supportTicketService.addReply(email, id, request);
            return ResponseEntity.status(201).body(reply);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request adding reply to ticket {}: {}", id, e.getMessage());
            return errorResponse(400, e.getMessage());
        } catch (SecurityException e) {
            logger.warn("Access denied adding reply to ticket {}: {}", id, e.getMessage());
            return errorResponse(403, e.getMessage());
        } catch (Exception e) {
            logger.error("Error adding reply to ticket {}", id, e);
            return errorResponse(500, "Failed to add reply");
        }
    }

    @GetMapping("/by-project/{projectId}")
    public ResponseEntity<Map<String, Object>> getTicketsByProject(@PathVariable Long projectId,
                                                 Authentication authentication) {
        try {
            String email = authentication.getName();
            List<?> tickets = supportTicketService.listByProjectForCustomer(projectId, email);
            return ResponseEntity.ok(Map.of("tickets", tickets));
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request listing tickets for project {}: {}", projectId, e.getMessage());
            return errorResponse(400, e.getMessage());
        } catch (Exception e) {
            logger.error("Error listing tickets for project {}", projectId, e);
            return errorResponse(500, "Failed to list tickets for project");
        }
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<Map<String, Object>> closeTicket(@PathVariable Long id,
                                         Authentication authentication) {
        try {
            String email = authentication.getName();
            Map<String, Object> ticket = supportTicketService.closeTicket(email, id);
            return ResponseEntity.ok(ticket);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request closing ticket {}: {}", id, e.getMessage());
            return errorResponse(400, e.getMessage());
        } catch (SecurityException e) {
            logger.warn("Access denied closing ticket {}: {}", id, e.getMessage());
            return errorResponse(403, e.getMessage());
        } catch (Exception e) {
            logger.error("Error closing ticket {}", id, e);
            return errorResponse(500, "Failed to close ticket");
        }
    }
}
