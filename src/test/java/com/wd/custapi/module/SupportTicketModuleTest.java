package com.wd.custapi.module;

import com.wd.custapi.config.TestDataSeeder;
import com.wd.custapi.model.SupportTicket;
import com.wd.custapi.repository.SupportTicketRepository;
import com.wd.custapi.support.AuthTestHelper;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Support Ticket endpoints (/api/support/tickets).
 * Covers create, list, get details, add reply, and close ticket.
 *
 * Note: SupportTicketController uses trailing slashes for POST and GET list
 * (e.g., POST /api/support/tickets/, GET /api/support/tickets/).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SupportTicketModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired TestDataSeeder seeder;
    @Autowired SupportTicketRepository ticketRepository;
    AuthTestHelper auth;

    /** Shared ticket ID across ordered tests. */
    private static Long createdTicketId;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String ticketsUrl() {
        return baseUrl() + "/api/support/tickets/";
    }

    // ---- Create Ticket ----

    @Test
    @Order(1)
    @SuppressWarnings("unchecked")
    void createTicket_validData_returns201() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", "Water leakage in bathroom");
        body.put("description", "There is water seepage from the bathroom ceiling on the first floor.");
        body.put("category", "DEFECT");
        body.put("priority", "HIGH");

        ResponseEntity<Map> response = restTemplate.exchange(
                ticketsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");

        // Store the ticket ID for subsequent tests
        Object idObj = response.getBody().get("id");
        createdTicketId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
    }

    @Test
    @Order(2)
    void createTicket_missingSubject_returnsBadRequest() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("description", "Missing subject field");
        body.put("category", "GENERAL");

        ResponseEntity<Map> response = restTemplate.exchange(
                ticketsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- List Tickets ----

    @Test
    @Order(3)
    void listTickets_authenticated_returnsTickets() {
        // Ensure at least one ticket exists
        if (createdTicketId == null) {
            createTicketForTest();
        }

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                ticketsUrl(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("tickets");
        assertThat(response.getBody()).containsKey("totalElements");
    }

    @Test
    @Order(4)
    void listTickets_unauthenticated_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                ticketsUrl(),
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        // Spring Security returns 403 for unauthenticated requests when no httpBasic/formLogin is configured
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    // ---- Get Ticket Details ----

    @Test
    @Order(5)
    void getTicketDetail_existingTicket_returnsDetails() {
        if (createdTicketId == null) {
            createTicketForTest();
        }

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/support/tickets/" + createdTicketId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody()).containsKey("subject");
    }

    // ---- Add Reply ----

    @Test
    @Order(6)
    void addReply_existingTicket_returns201() {
        if (createdTicketId == null) {
            createTicketForTest();
        }

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        Map<String, String> body = Map.of(
                "message", "Thank you for looking into this. Please expedite the fix.");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/support/tickets/" + createdTicketId + "/replies",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
    }

    // ---- Close Ticket ----

    @Test
    @Order(7)
    void closeTicket_existingTicket_returnsOk() {
        if (createdTicketId == null) {
            createTicketForTest();
        }

        // Customer can only close RESOLVED tickets — simulate staff resolving it.
        SupportTicket ticket = ticketRepository.findById(createdTicketId).orElseThrow();
        ticket.setStatus("RESOLVED");
        ticketRepository.save(ticket);

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/support/tickets/" + createdTicketId + "/close",
                HttpMethod.PUT,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("status");
    }

    // ---- Helper ----

    @SuppressWarnings("unchecked")
    private void createTicketForTest() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", "Test ticket for subsequent tests");
        body.put("description", "Auto-created test ticket");
        body.put("category", "GENERAL");
        body.put("priority", "MEDIUM");

        ResponseEntity<Map> response = restTemplate.exchange(
                ticketsUrl(),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
            Object idObj = response.getBody().get("id");
            createdTicketId = idObj instanceof Number ? ((Number) idObj).longValue() : Long.parseLong(idObj.toString());
        }
    }
}
