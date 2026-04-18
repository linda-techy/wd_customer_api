package com.wd.custapi.service;

import com.wd.custapi.dto.SupportTicketReplyRequest;
import com.wd.custapi.dto.SupportTicketRequest;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.SupportTicket;
import com.wd.custapi.model.SupportTicketReply;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.SupportTicketRepository;
import com.wd.custapi.repository.SupportTicketReplyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SupportTicketService {

    private static final Logger logger = LoggerFactory.getLogger(SupportTicketService.class);

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketReplyRepository replyRepository;
    private final CustomerUserRepository userRepository;

    public SupportTicketService(SupportTicketRepository ticketRepository,
                                SupportTicketReplyRepository replyRepository,
                                CustomerUserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.replyRepository = replyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> createTicket(String email, SupportTicketRequest request) {
        CustomerUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        Long seq = ticketRepository.getNextTicketSequence();
        String ticketNumber = String.format("TKT-%05d", seq);

        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNumber(ticketNumber);
        ticket.setCustomerUser(user);
        ticket.setProjectId(request.getProjectId());
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setCategory(request.getCategory() != null ? request.getCategory() : "GENERAL");
        ticket.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        ticket.setStatus("OPEN");

        ticket = ticketRepository.save(ticket);
        logger.info("Created support ticket {} for user {}", ticketNumber, email);

        return toTicketDto(ticket);
    }

    public Page<Map<String, Object>> getMyTickets(String email, String status, int page, int size) {
        CustomerUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets;

        if (status != null && !status.isBlank()) {
            tickets = ticketRepository.findByCustomerUser_IdAndStatusOrderByUpdatedAtDesc(user.getId(), status.toUpperCase(), pageable);
        } else {
            tickets = ticketRepository.findByCustomerUser_IdOrderByUpdatedAtDesc(user.getId(), pageable);
        }

        return tickets.map(this::toTicketDto);
    }

    public Map<String, Object> getTicketDetail(String email, Long ticketId) {
        CustomerUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (!ticket.getCustomerUser().getId().equals(user.getId())) {
            throw new SecurityException("Access denied to ticket: " + ticketId);
        }

        List<SupportTicketReply> replies = replyRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId);

        Map<String, Object> result = toTicketDto(ticket);
        result.put("replies", replies.stream().map(this::toReplyDto).collect(Collectors.toList()));

        return result;
    }

    @Transactional
    public Map<String, Object> addReply(String email, Long ticketId, SupportTicketReplyRequest request) {
        CustomerUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (!ticket.getCustomerUser().getId().equals(user.getId())) {
            throw new SecurityException("Access denied to ticket: " + ticketId);
        }

        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setUserId(user.getId());
        reply.setUserType("CUSTOMER");
        reply.setUserName(user.getFirstName() + " " + user.getLastName());
        reply.setMessage(request.getMessage());
        reply.setAttachmentUrl(request.getAttachmentUrl());

        // Reopen ticket if it was resolved when customer replies
        if ("RESOLVED".equals(ticket.getStatus())) {
            ticket.setStatus("OPEN");
            ticketRepository.save(ticket);
        }

        reply = replyRepository.save(reply);
        logger.info("Added reply to ticket {} by user {}", ticketId, email);

        return toReplyDto(reply);
    }

    @Transactional
    public Map<String, Object> closeTicket(String email, Long ticketId) {
        CustomerUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (!ticket.getCustomerUser().getId().equals(user.getId())) {
            throw new SecurityException("Access denied to ticket: " + ticketId);
        }

        if (!"RESOLVED".equals(ticket.getStatus())) {
            throw new IllegalArgumentException("Only RESOLVED tickets can be closed by the customer");
        }

        ticket.setStatus("CLOSED");
        ticket.setClosedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);
        logger.info("Closed ticket {} by user {}", ticketId, email);

        return toTicketDto(ticket);
    }

    public Map<String, Object> toTicketDto(SupportTicket ticket) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", ticket.getId());
        map.put("ticketNumber", ticket.getTicketNumber());
        map.put("subject", ticket.getSubject());
        map.put("description", ticket.getDescription());
        map.put("category", ticket.getCategory());
        map.put("priority", ticket.getPriority());
        map.put("status", ticket.getStatus());
        map.put("projectId", ticket.getProjectId());
        map.put("assignedTo", ticket.getAssignedTo());
        map.put("createdAt", ticket.getCreatedAt());
        map.put("updatedAt", ticket.getUpdatedAt());
        map.put("resolvedAt", ticket.getResolvedAt());
        map.put("closedAt", ticket.getClosedAt());
        return map;
    }

    private Map<String, Object> toReplyDto(SupportTicketReply reply) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reply.getId());
        map.put("ticketId", reply.getTicket().getId());
        map.put("userId", reply.getUserId());
        map.put("userType", reply.getUserType());
        map.put("userName", reply.getUserName());
        map.put("message", reply.getMessage());
        map.put("attachmentUrl", reply.getAttachmentUrl());
        map.put("createdAt", reply.getCreatedAt());
        return map;
    }
}
