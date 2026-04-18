package com.wd.custapi.service;

import com.wd.custapi.dto.SupportTicketReplyRequest;
import com.wd.custapi.dto.SupportTicketRequest;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.SupportTicket;
import com.wd.custapi.model.SupportTicketReply;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.SupportTicketReplyRepository;
import com.wd.custapi.repository.SupportTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock
    private SupportTicketRepository ticketRepository;

    @Mock
    private SupportTicketReplyRepository replyRepository;

    @Mock
    private CustomerUserRepository userRepository;

    @InjectMocks
    private SupportTicketService supportTicketService;

    private CustomerUser user;

    @BeforeEach
    void setUp() {
        user = new CustomerUser();
        user.setId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
    }

    // ── createTicket ──────────────────────────────────────────────────────────

    @Test
    void createTicket_validRequest_savesTicketWithCorrectFields() {
        SupportTicketRequest request = new SupportTicketRequest();
        request.setSubject("Roof issue");
        request.setDescription("There is a leak");
        request.setCategory("CONSTRUCTION");
        request.setPriority("HIGH");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(ticketRepository.getNextTicketSequence()).thenReturn(42L);

        SupportTicket saved = new SupportTicket();
        saved.setId(10L);
        saved.setTicketNumber("TKT-00042");
        saved.setSubject("Roof issue");
        saved.setDescription("There is a leak");
        saved.setCategory("CONSTRUCTION");
        saved.setPriority("HIGH");
        saved.setStatus("OPEN");
        saved.setCustomerUser(user);
        when(ticketRepository.save(any(SupportTicket.class))).thenReturn(saved);

        Map<String, Object> result = supportTicketService.createTicket("john@example.com", request);

        ArgumentCaptor<SupportTicket> captor = ArgumentCaptor.forClass(SupportTicket.class);
        verify(ticketRepository).save(captor.capture());

        SupportTicket captured = captor.getValue();
        assertEquals("TKT-00042", captured.getTicketNumber());
        assertEquals("OPEN", captured.getStatus());
        assertEquals("CONSTRUCTION", captured.getCategory());
        assertEquals("HIGH", captured.getPriority());
        assertEquals(user, captured.getCustomerUser());

        assertNotNull(result);
        assertEquals("TKT-00042", result.get("ticketNumber"));
    }

    @Test
    void createTicket_noCategory_defaultsToGeneral() {
        SupportTicketRequest request = new SupportTicketRequest();
        request.setSubject("General question");
        request.setDescription("I need help");
        // category and priority intentionally null

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(ticketRepository.getNextTicketSequence()).thenReturn(1L);

        SupportTicket saved = new SupportTicket();
        saved.setId(1L);
        saved.setTicketNumber("TKT-00001");
        saved.setSubject("General question");
        saved.setDescription("I need help");
        saved.setCategory("GENERAL");
        saved.setPriority("MEDIUM");
        saved.setStatus("OPEN");
        saved.setCustomerUser(user);
        when(ticketRepository.save(any(SupportTicket.class))).thenReturn(saved);

        supportTicketService.createTicket("john@example.com", request);

        ArgumentCaptor<SupportTicket> captor = ArgumentCaptor.forClass(SupportTicket.class);
        verify(ticketRepository).save(captor.capture());
        assertEquals("GENERAL", captor.getValue().getCategory());
        assertEquals("MEDIUM", captor.getValue().getPriority());
    }

    @Test
    void createTicket_unknownUser_throwsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        SupportTicketRequest request = new SupportTicketRequest();
        request.setSubject("Test");
        request.setDescription("Test");

        assertThrows(IllegalArgumentException.class,
                () -> supportTicketService.createTicket("unknown@example.com", request));
        verify(ticketRepository, never()).save(any());
    }

    // ── closeTicket ───────────────────────────────────────────────────────────

    @Test
    void closeTicket_resolvedTicket_closesSuccessfully() {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(5L);
        ticket.setStatus("RESOLVED");
        ticket.setCustomerUser(user);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = supportTicketService.closeTicket("john@example.com", 5L);

        assertEquals("CLOSED", ticket.getStatus());
        assertNotNull(ticket.getClosedAt());
    }

    @Test
    void closeTicket_openTicket_throwsIllegalArgumentException() {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(5L);
        ticket.setStatus("OPEN");
        ticket.setCustomerUser(user);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalArgumentException.class,
                () -> supportTicketService.closeTicket("john@example.com", 5L));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void closeTicket_inProgressTicket_throwsIllegalArgumentException() {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(5L);
        ticket.setStatus("IN_PROGRESS");
        ticket.setCustomerUser(user);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalArgumentException.class,
                () -> supportTicketService.closeTicket("john@example.com", 5L));
    }

    // ── ownership validation ──────────────────────────────────────────────────

    @Test
    void closeTicket_differentOwner_throwsSecurityException() {
        CustomerUser otherUser = new CustomerUser();
        otherUser.setId(99L);

        SupportTicket ticket = new SupportTicket();
        ticket.setId(5L);
        ticket.setStatus("RESOLVED");
        ticket.setCustomerUser(otherUser);  // owned by different user

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        assertThrows(SecurityException.class,
                () -> supportTicketService.closeTicket("john@example.com", 5L));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void getTicketDetail_differentOwner_throwsSecurityException() {
        CustomerUser otherUser = new CustomerUser();
        otherUser.setId(99L);

        SupportTicket ticket = new SupportTicket();
        ticket.setId(5L);
        ticket.setStatus("OPEN");
        ticket.setCustomerUser(otherUser);

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));

        assertThrows(SecurityException.class,
                () -> supportTicketService.getTicketDetail("john@example.com", 5L));
    }

    @Test
    void addReply_resolvedTicket_reopensToOpen() {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(5L);
        ticket.setStatus("RESOLVED");
        ticket.setCustomerUser(user);

        SupportTicketReplyRequest request = new SupportTicketReplyRequest();
        request.setMessage("Still having the issue");

        SupportTicketReply reply = new SupportTicketReply();
        reply.setId(1L);
        reply.setTicket(ticket);
        reply.setMessage("Still having the issue");
        reply.setUserId(1L);
        reply.setUserType("CUSTOMER");
        reply.setUserName("John Doe");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(SupportTicket.class))).thenReturn(ticket);
        when(replyRepository.save(any(SupportTicketReply.class))).thenReturn(reply);

        supportTicketService.addReply("john@example.com", 5L, request);

        // Ticket should be reopened
        verify(ticketRepository).save(ticket);
        assertEquals("OPEN", ticket.getStatus());
    }
}
