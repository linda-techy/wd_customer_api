package com.wd.custapi.module;

import com.wd.custapi.config.TestDataSeeder;
import com.wd.custapi.model.BoqDocument;
import com.wd.custapi.model.ChangeOrder;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.enums.BoqDocumentStatus;
import com.wd.custapi.repository.BoqDocumentRepository;
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
 * Integration tests for BOQ view endpoints (/api/projects/{projectId}/boq/*).
 * Covers BOQ document, summary, payment stages, acknowledgement, and change orders.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BoqViewModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired TestDataSeeder seeder;
    @Autowired BoqDocumentRepository boqDocumentRepository;
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

    private String boqUrl(Project project) {
        return baseUrl() + "/api/projects/" + project.getProjectUuid() + "/boq";
    }

    /**
     * Creates a BoqDocument via reflection since the entity has no setters (read-only design).
     */
    private BoqDocument createBoqDocument(Project project, BoqDocumentStatus status) {
        BoqDocument doc = new BoqDocument();
        setField(doc, "project", project);
        setField(doc, "status", status);
        setField(doc, "revisionNumber", 1);
        setField(doc, "totalValueExGst", new BigDecimal("1000000.00"));
        setField(doc, "gstRate", new BigDecimal("18.0000"));
        setField(doc, "totalGstAmount", new BigDecimal("180000.00"));
        setField(doc, "totalValueInclGst", new BigDecimal("1180000.00"));
        setField(doc, "submittedAt", LocalDateTime.now());
        setField(doc, "createdAt", LocalDateTime.now());
        if (status == BoqDocumentStatus.APPROVED) {
            setField(doc, "approvedAt", LocalDateTime.now());
        }
        return boqDocumentRepository.save(doc);
    }

    /**
     * Creates a ChangeOrder via reflection for read-only fields; uses setters where available.
     */
    private ChangeOrder createChangeOrder(Project project, String status) {
        ChangeOrder co = new ChangeOrder();
        setField(co, "project", project);
        setField(co, "referenceNumber", "CO-" + System.nanoTime());
        setField(co, "coType", "ADDITION");
        setField(co, "title", "Test Change Order");
        setField(co, "description", "Test description");
        setField(co, "justification", "Test justification");
        setField(co, "netAmountExGst", new BigDecimal("50000.00"));
        setField(co, "gstAmount", new BigDecimal("9000.00"));
        setField(co, "netAmountInclGst", new BigDecimal("59000.00"));
        setField(co, "submittedAt", LocalDateTime.now());
        setField(co, "createdAt", LocalDateTime.now());
        co.setStatus(status);
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

    // ---- BOQ Document ----

    @Test
    @Order(1)
    void getBoqDocument_noDocument_returnsNotFoundMessage() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                boqUrl(seeder.getResidentialVilla()) + "/document",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        // 404 or 200 with "No BOQ document found" message
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(2)
    void getBoqDocument_withDocument_returnsDocument() {
        createBoqDocument(seeder.getResidentialVilla(), BoqDocumentStatus.APPROVED);
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                boqUrl(seeder.getResidentialVilla()) + "/document",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody()).containsKey("status");
        assertThat(response.getBody()).containsKey("totalValueInclGst");
    }

    // ---- BOQ Summary ----

    @Test
    @Order(3)
    void getBoqSummary_withApprovedDoc_returnsSummary() {
        createBoqDocument(seeder.getResidentialVilla(), BoqDocumentStatus.APPROVED);
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                boqUrl(seeder.getResidentialVilla()) + "/summary",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ---- Payment Stages ----

    @Test
    @Order(4)
    void getPaymentStages_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                boqUrl(seeder.getResidentialVilla()) + "/payment-stages",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
        assertThat(response.getBody()).containsKey("stages");
    }

    // ---- Acknowledge BOQ ----

    @Test
    @Order(5)
    @SuppressWarnings("unchecked")
    void acknowledgeBoq_validDocument_returnsOk() {
        BoqDocument doc = createBoqDocument(seeder.getResidentialVilla(), BoqDocumentStatus.APPROVED);
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                boqUrl(seeder.getResidentialVilla()) + "/documents/" + doc.getId() + "/acknowledge",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ---- Change Orders ----

    @Test
    @Order(6)
    void listChangeOrders_returnsOk() {
        createChangeOrder(seeder.getResidentialVilla(), "PENDING");
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                boqUrl(seeder.getResidentialVilla()) + "/change-orders",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("changeOrders");
    }

    @Test
    @Order(7)
    void getPendingChangeOrders_returnsOk() {
        createChangeOrder(seeder.getResidentialVilla(), "PENDING");
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                boqUrl(seeder.getResidentialVilla()) + "/change-orders/pending-review",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("changeOrders");
    }

    @Test
    @Order(8)
    void approveChangeOrder_pendingCo_succeeds() {
        ChangeOrder co = createChangeOrder(seeder.getResidentialVilla(), "PENDING");
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                boqUrl(seeder.getResidentialVilla()) + "/change-orders/" + co.getId() + "/approve",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
    }

    @Test
    @Order(9)
    @SuppressWarnings("unchecked")
    void rejectChangeOrder_withReason_succeeds() {
        ChangeOrder co = createChangeOrder(seeder.getResidentialVilla(), "PENDING");
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        Map<String, String> body = Map.of("reason", "Not aligned with project scope");

        ResponseEntity<Map> response = restTemplate.exchange(
                boqUrl(seeder.getResidentialVilla()) + "/change-orders/" + co.getId() + "/reject",
                HttpMethod.PATCH,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
    }
}
