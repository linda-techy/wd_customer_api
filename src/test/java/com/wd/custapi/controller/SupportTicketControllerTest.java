package com.wd.custapi.controller;

import com.wd.custapi.dto.SupportTicketReplyRequest;
import com.wd.custapi.dto.SupportTicketRequest;
import com.wd.custapi.service.SupportTicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Direct-invocation Mockito unit tests for {@link SupportTicketController}.
 *
 * <p>Covers every endpoint's happy path plus each declared catch branch
 * (400 IllegalArgumentException / 403 SecurityException / 500 generic).
 * The single collaborator {@link SupportTicketService} and the
 * {@link Authentication} are mocked. No Spring / MockMvc / DB.
 */
@ExtendWith(MockitoExtension.class)
class SupportTicketControllerTest {

    private static final String EMAIL = "customer@example.com";

    @Mock private SupportTicketService supportTicketService;
    @Mock private Authentication auth;

    @InjectMocks private SupportTicketController controller;

    @BeforeEach
    void setUp() {
        when(auth.getName()).thenReturn(EMAIL);
    }

    private SupportTicketRequest ticketRequest() {
        SupportTicketRequest r = new SupportTicketRequest();
        r.setSubject("Leaking tap");
        r.setDescription("Tap in kitchen leaks");
        return r;
    }

    private SupportTicketReplyRequest replyRequest() {
        SupportTicketReplyRequest r = new SupportTicketReplyRequest();
        r.setMessage("Any update?");
        return r;
    }

    // ---- POST / (createTicket) ----

    @Test
    void createTicket_success_returns201WithBody() {
        Map<String, Object> created = Map.of("id", 5L, "status", "OPEN");
        when(supportTicketService.createTicket(eq(EMAIL), any())).thenReturn(created);

        ResponseEntity<Map<String, Object>> response = controller.createTicket(ticketRequest(), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).containsEntry("id", 5L);
    }

    @Test
    void createTicket_illegalArgument_returns400() {
        when(supportTicketService.createTicket(eq(EMAIL), any()))
                .thenThrow(new IllegalArgumentException("subject required"));

        ResponseEntity<Map<String, Object>> response = controller.createTicket(ticketRequest(), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "subject required");
    }

    @Test
    void createTicket_unexpected_returns500() {
        when(supportTicketService.createTicket(eq(EMAIL), any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.createTicket(ticketRequest(), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Failed to create support ticket");
    }

    // ---- GET / (getMyTickets) ----

    @Test
    void getMyTickets_success_returnsPagedEnvelope() {
        Page<Map<String, Object>> page = new PageImpl<>(
                List.of(Map.of("id", 1L)), PageRequest.of(0, 10), 1);
        when(supportTicketService.getMyTickets(EMAIL, null, 0, 10)).thenReturn(page);

        ResponseEntity<Map<String, Object>> response = controller.getMyTickets(0, 10, null, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("totalElements", 1L);
        assertThat(response.getBody()).containsEntry("totalPages", 1);
        assertThat(response.getBody()).containsEntry("page", 0);
        assertThat(response.getBody()).containsEntry("size", 10);
        assertThat(response.getBody().get("tickets")).isInstanceOf(List.class);
    }

    @Test
    void getMyTickets_illegalArgument_returns400() {
        when(supportTicketService.getMyTickets(EMAIL, "BOGUS", 0, 10))
                .thenThrow(new IllegalArgumentException("invalid status"));

        ResponseEntity<Map<String, Object>> response = controller.getMyTickets(0, 10, "BOGUS", auth);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "invalid status");
    }

    @Test
    void getMyTickets_unexpected_returns500() {
        when(supportTicketService.getMyTickets(EMAIL, null, 0, 10))
                .thenThrow(new RuntimeException("db down"));

        ResponseEntity<Map<String, Object>> response = controller.getMyTickets(0, 10, null, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Failed to list support tickets");
    }

    // ---- GET /{id} (getTicketDetail) ----

    @Test
    void getTicketDetail_success_returns200() {
        when(supportTicketService.getTicketDetail(EMAIL, 9L)).thenReturn(Map.of("id", 9L));

        ResponseEntity<Map<String, Object>> response = controller.getTicketDetail(9L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("id", 9L);
    }

    @Test
    void getTicketDetail_illegalArgument_returns400() {
        when(supportTicketService.getTicketDetail(EMAIL, 9L))
                .thenThrow(new IllegalArgumentException("not found"));

        ResponseEntity<Map<String, Object>> response = controller.getTicketDetail(9L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "not found");
    }

    @Test
    void getTicketDetail_securityException_returns403() {
        when(supportTicketService.getTicketDetail(EMAIL, 9L))
                .thenThrow(new SecurityException("not your ticket"));

        ResponseEntity<Map<String, Object>> response = controller.getTicketDetail(9L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).containsEntry("error", "not your ticket");
    }

    @Test
    void getTicketDetail_unexpected_returns500() {
        when(supportTicketService.getTicketDetail(EMAIL, 9L))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.getTicketDetail(9L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Failed to fetch ticket");
    }

    // ---- POST /{id}/replies (addReply) ----

    @Test
    void addReply_success_returns201() {
        when(supportTicketService.addReply(eq(EMAIL), eq(9L), any()))
                .thenReturn(Map.of("replyId", 3L));

        ResponseEntity<Map<String, Object>> response = controller.addReply(9L, replyRequest(), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).containsEntry("replyId", 3L);
    }

    @Test
    void addReply_illegalArgument_returns400() {
        when(supportTicketService.addReply(eq(EMAIL), eq(9L), any()))
                .thenThrow(new IllegalArgumentException("empty message"));

        ResponseEntity<Map<String, Object>> response = controller.addReply(9L, replyRequest(), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "empty message");
    }

    @Test
    void addReply_securityException_returns403() {
        when(supportTicketService.addReply(eq(EMAIL), eq(9L), any()))
                .thenThrow(new SecurityException("denied"));

        ResponseEntity<Map<String, Object>> response = controller.addReply(9L, replyRequest(), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).containsEntry("error", "denied");
    }

    @Test
    void addReply_unexpected_returns500() {
        when(supportTicketService.addReply(eq(EMAIL), eq(9L), any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.addReply(9L, replyRequest(), auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Failed to add reply");
    }

    // ---- GET /by-project/{projectId} (getTicketsByProject) ----

    @Test
    void getTicketsByProject_success_returnsTicketsList() {
        when(supportTicketService.listByProjectForCustomer(50L, EMAIL))
                .thenReturn(List.of(Map.of("id", 1L), Map.of("id", 2L)));

        ResponseEntity<Map<String, Object>> response = controller.getTicketsByProject(50L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat((List<?>) response.getBody().get("tickets")).hasSize(2);
    }

    @Test
    void getTicketsByProject_illegalArgument_returns400() {
        when(supportTicketService.listByProjectForCustomer(50L, EMAIL))
                .thenThrow(new IllegalArgumentException("not a member"));

        ResponseEntity<Map<String, Object>> response = controller.getTicketsByProject(50L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "not a member");
    }

    @Test
    void getTicketsByProject_unexpected_returns500() {
        when(supportTicketService.listByProjectForCustomer(50L, EMAIL))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.getTicketsByProject(50L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Failed to list tickets for project");
    }

    // ---- PUT /{id}/close (closeTicket) ----

    @Test
    void closeTicket_success_returns200() {
        when(supportTicketService.closeTicket(EMAIL, 9L)).thenReturn(Map.of("id", 9L, "status", "CLOSED"));

        ResponseEntity<Map<String, Object>> response = controller.closeTicket(9L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "CLOSED");
    }

    @Test
    void closeTicket_illegalArgument_returns400() {
        when(supportTicketService.closeTicket(EMAIL, 9L))
                .thenThrow(new IllegalArgumentException("already closed"));

        ResponseEntity<Map<String, Object>> response = controller.closeTicket(9L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("error", "already closed");
    }

    @Test
    void closeTicket_securityException_returns403() {
        when(supportTicketService.closeTicket(EMAIL, 9L))
                .thenThrow(new SecurityException("denied"));

        ResponseEntity<Map<String, Object>> response = controller.closeTicket(9L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).containsEntry("error", "denied");
    }

    @Test
    void closeTicket_unexpected_returns500() {
        when(supportTicketService.closeTicket(EMAIL, 9L))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.closeTicket(9L, auth);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Failed to close ticket");
    }
}
