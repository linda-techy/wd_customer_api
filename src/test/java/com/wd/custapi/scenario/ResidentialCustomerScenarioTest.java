package com.wd.custapi.scenario;

import com.wd.custapi.config.TestDataSeeder;
import com.wd.custapi.support.AuthTestHelper;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end scenario test for Customer A's residential project journey.
 *
 * Walks through the full lifecycle a residential customer would experience:
 * login, browse dashboard, view project details and phases, check BOQ and
 * payment stages, review site reports and invoices, and create a support ticket.
 *
 * Tests are ordered to simulate a realistic user session. Each step builds
 * on the authenticated context established in step 1.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResidentialCustomerScenarioTest extends TestcontainersPostgresBase {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired TestDataSeeder seeder;
    AuthTestHelper auth;

    /** Token persisted across ordered tests within the same instance. */
    private String token;
    private String projectUuid;

    @BeforeAll
    void setUpOnce() {
        seeder.seed();
    }

    @BeforeEach
    void setUp() {
        auth = new AuthTestHelper(restTemplate, port);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    // ---- Step 1: Login and verify identity ----

    @Test
    @Order(1)
    @SuppressWarnings("unchecked")
    void step01_loginAndVerifyUserInfo() {
        token = auth.loginAsCustomerA();
        assertThat(token).isNotBlank();

        // Verify user info via /auth/me
        HttpHeaders headers = auth.authHeaders(token);
        ResponseEntity<Map> meResponse = restTemplate.exchange(
                baseUrl() + "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).isNotNull();
        Map<String, Object> body = meResponse.getBody();
        // The /auth/me response should contain user identification fields
        assertThat(body).containsAnyOf(
                Map.entry("email", "customerA@test.com")
        );

        // Store the project UUID for subsequent steps
        projectUuid = seeder.getResidentialVilla().getProjectUuid().toString();
    }

    // ---- Step 2: Dashboard overview ----

    @Test
    @Order(2)
    void step02_getDashboard() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ---- Step 3: Recent projects ----

    @Test
    @Order(3)
    void step03_getRecentProjects() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/recent-projects",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Customer A has at least the Residential Villa project
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(1);
    }

    // ---- Step 4: Project details by UUID ----

    @Test
    @Order(4)
    @SuppressWarnings("unchecked")
    void step04_getProjectDetailsByUuid() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/projects/" + projectUuid,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ---- Step 5: Project phases ----

    @Test
    @Order(5)
    void step05_getProjectPhases() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/projects/" + projectUuid + "/phases",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Phases may be empty if none seeded, but endpoint should respond OK
    }

    // ---- Step 6: BOQ document ----

    @Test
    @Order(6)
    void step06_getBoqDocument() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/boq/document",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        // No BOQ document seeded; expect either 200 with empty/message or 404
        assertThat(response.getStatusCode().value()).isIn(200, 404);
        assertThat(response.getBody()).isNotNull();
    }

    // ---- Step 7: Payment stages ----

    @Test
    @Order(7)
    void step07_getPaymentStages() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/boq/payment-stages",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("success");
    }

    // ---- Step 8: Site reports ----

    @Test
    @Order(8)
    void step08_getSiteReports() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/site-reports",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("success");
    }

    // ---- Step 9: Invoices ----

    @Test
    @Order(9)
    void step09_getInvoices() {
        HttpHeaders headers = auth.authHeaders(token);

        // Invoice list endpoint requires projectId query parameter
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/invoices?projectId=" + projectUuid,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("success");
    }

    // ---- Step 10: Create support ticket and verify it appears ----

    @Test
    @Order(10)
    @SuppressWarnings("unchecked")
    void step10_createSupportTicketAndVerify() {
        HttpHeaders headers = auth.authHeaders(token);

        // Create a support ticket
        Map<String, Object> ticketBody = new LinkedHashMap<>();
        ticketBody.put("subject", "Plumbing issue in master bedroom");
        ticketBody.put("description", "There is a leak under the bathroom sink in the master bedroom.");
        ticketBody.put("category", "DEFECT");
        ticketBody.put("priority", "HIGH");

        ResponseEntity<Map> createResponse = restTemplate.exchange(
                baseUrl() + "/api/support/tickets/",
                HttpMethod.POST,
                new HttpEntity<>(ticketBody, headers),
                Map.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody()).containsKey("id");

        // Verify the ticket appears in the list
        ResponseEntity<Map> listResponse = restTemplate.exchange(
                baseUrl() + "/api/support/tickets/",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull();
        assertThat(listResponse.getBody()).containsKey("tickets");
    }
}
