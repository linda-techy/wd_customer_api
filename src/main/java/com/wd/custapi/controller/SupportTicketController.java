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

import java.util.Map;

@RestController
@RequestMapping("/api/support/tickets")
public class SupportTicketController {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketController.class);

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping("/")
    public ResponseEntity<?> createTicket(@Valid @RequestBody SupportTicketRequest request,
                                          Authentication authentication) {
        try {
            String email = authentication.getName();
            Map<String, Object> ticket = supportTicketService.createTicket(email, request);
            return ResponseEntity.status(201).body(ticket);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request creating support ticket: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating support ticket", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create support ticket"));
        }
    }

    @GetMapping("/")
    public ResponseEntity<?> getMyTickets(@RequestParam(defaultValue = "0") int page,
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error listing support tickets", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to list support tickets"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTicketDetail(@PathVariable Long id,
                                             Authentication authentication) {
        try {
            String email = authentication.getName();
            Map<String, Object> ticket = supportTicketService.getTicketDetail(email, id);
            return ResponseEntity.ok(ticket);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request fetching ticket {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("Access denied for ticket {}: {}", id, e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching ticket {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch ticket"));
        }
    }

    @PostMapping("/{id}/replies")
    public ResponseEntity<?> addReply(@PathVariable Long id,
                                      @Valid @RequestBody SupportTicketReplyRequest request,
                                      Authentication authentication) {
        try {
            String email = authentication.getName();
            Map<String, Object> reply = supportTicketService.addReply(email, id, request);
            return ResponseEntity.status(201).body(reply);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request adding reply to ticket {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("Access denied adding reply to ticket {}: {}", id, e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error adding reply to ticket {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to add reply"));
        }
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<?> closeTicket(@PathVariable Long id,
                                         Authentication authentication) {
        try {
            String email = authentication.getName();
            Map<String, Object> ticket = supportTicketService.closeTicket(email, id);
            return ResponseEntity.ok(ticket);
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request closing ticket {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            logger.warn("Access denied closing ticket {}: {}", id, e.getMessage());
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error closing ticket {}", id, e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to close ticket"));
        }
    }
}
