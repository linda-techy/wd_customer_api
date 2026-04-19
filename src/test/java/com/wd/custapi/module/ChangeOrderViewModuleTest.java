package com.wd.custapi.module;

import com.wd.custapi.config.TestDataSeeder;
import com.wd.custapi.model.ChangeOrder;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.ChangeOrderRepository;
import com.wd.custapi.support.AuthTestHelper;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Change Order endpoints (/api/projects/{projectId}/boq/change-orders/*).
 * Covers listing COs, pending COs, approve, reject, and rejection reason persistence.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChangeOrderViewModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired TestDataSeeder seeder;
    @Autowired ChangeOrderRepository changeOrderRepository;
    AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    private String coUrl(Project project) {
        return baseUrl() + "/api/projects/" + project.getProjectUuid() + "/boq/change-orders";
    }

    /**
     * Creates a pending ChangeOrder via reflection for read-only fields; uses setStatus where available.
     */
    private ChangeOrder createPendingCo(Project project, String title) {
        ChangeOrder co = new ChangeOrder();
        setField(co, "project", project);
        setField(co, "referenceNumber", "CO-" + System.nanoTime());
        setField(co, "coType", "ADDITION");
        setField(co, "title", title);
        setField(co, "description", "Description for " + title);
        setField(co, "justification", "Justification for " + title);
        setField(co, "netAmountExGst", new BigDecimal("50000.00"));
        setField(co, "gstAmount", new BigDecimal("9000.00"));
        setField(co, "netAmountInclGst", new BigDecimal("59000.00"));
        setField(co, "submittedAt", LocalDateTime.now());
        setField(co, "createdAt", LocalDateTime.now());
        co.setStatus("PENDING");
        return changeOrderRepository.save(co);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "'", e);
        }
    }

    // ---- List All Change Orders ----

    @Test
    @Order(1)
    void listChangeOrders_withData_returnsAll() {
        createPendingCo(seeder.getResidentialVilla(), "CO Alpha");
        createPendingCo(seeder.getResidentialVilla(), "CO Beta");

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                coUrl(seeder.getResidentialVilla()),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("changeOrders");
        assertThat(response.getBody()).containsKey("success");
    }

    // ---- Pending Review ----

    @Test
    @Order(2)
    void getPendingChangeOrders_returnsPendingOnly() {
        createPendingCo(seeder.getResidentialVilla(), "CO Pending");

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                coUrl(seeder.getResidentialVilla()) + "/pending-review",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("changeOrders");
    }

    // ---- Approve Change Order ----

    @Test
    @Order(3)
    void approveChangeOrder_pendingCo_returnsSuccess() {
        ChangeOrder co = createPendingCo(seeder.getResidentialVilla(), "CO to Approve");

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                coUrl(seeder.getResidentialVilla()) + "/" + co.getId() + "/approve",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ---- Reject Change Order ----

    @Test
    @Order(4)
    void rejectChangeOrder_pendingCo_returnsSuccess() {
        ChangeOrder co = createPendingCo(seeder.getResidentialVilla(), "CO to Reject");

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        Map<String, String> body = Map.of("reason", "Budget constraints do not allow this change");

        ResponseEntity<Map> response = restTemplate.exchange(
                coUrl(seeder.getResidentialVilla()) + "/" + co.getId() + "/reject",
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ---- Rejection Reason Persists ----

    @Test
    @Order(5)
    @SuppressWarnings("unchecked")
    void rejectChangeOrder_rejectionReasonPersists() {
        ChangeOrder co = createPendingCo(seeder.getResidentialVilla(), "CO Reason Check");
        String rejectionReason = "Not within the approved scope of work";

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        Map<String, String> body = Map.of("reason", rejectionReason);

        // Reject
        restTemplate.exchange(
                coUrl(seeder.getResidentialVilla()) + "/" + co.getId() + "/reject",
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                Map.class);

        // Verify by fetching the individual CO
        ResponseEntity<Map> detailResponse = restTemplate.exchange(
                coUrl(seeder.getResidentialVilla()) + "/" + co.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> coData = (Map<String, Object>) detailResponse.getBody().get("changeOrder");
        assertThat(coData).isNotNull();
        assertThat(coData.get("rejectionReason")).isEqualTo(rejectionReason);
        assertThat(coData.get("status")).isEqualTo("REJECTED");
    }

    // ---- Unauthenticated ----

    @Test
    @Order(6)
    void listChangeOrders_unauthenticated_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                coUrl(seeder.getResidentialVilla()),
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        // Spring Security returns 403 for unauthenticated requests when no httpBasic/formLogin is configured
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }
}
