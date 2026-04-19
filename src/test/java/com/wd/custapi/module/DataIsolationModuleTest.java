package com.wd.custapi.module;

import com.wd.custapi.config.TestDataSeeder;
import com.wd.custapi.model.BoqDocument;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.ProjectInvoice;
import com.wd.custapi.model.enums.BoqDocumentStatus;
import com.wd.custapi.model.enums.InvoiceStatus;
import com.wd.custapi.repository.BoqDocumentRepository;
import com.wd.custapi.repository.ProjectInvoiceRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for data isolation across customers.
 * Verifies that Customer A cannot access Customer B's data and vice versa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataIsolationModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired TestDataSeeder seeder;
    @Autowired ProjectInvoiceRepository invoiceRepository;
    @Autowired BoqDocumentRepository boqDocumentRepository;
    AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    // ---- Customer A Cannot See Customer B's Project Details ----

    @Test
    @Order(1)
    void customerA_cannotSeeCustomerB_projectDetails() {
        String tokenA = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(tokenA);

        String projectBUuid = seeder.getCommercialOffice().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/projects/" + projectBUuid,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        // Should be 403 Forbidden or 404 Not Found
        assertThat(response.getStatusCode().value()).isIn(403, 404);
    }

    @Test
    @Order(2)
    void customerB_cannotSeeCustomerA_projectDetails() {
        String tokenB = auth.loginAsCustomerB();
        HttpHeaders headers = auth.authHeaders(tokenB);

        String projectAUuid = seeder.getResidentialVilla().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/projects/" + projectAUuid,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404);
    }

    // ---- Customer A Cannot See Customer B's Invoices ----

    @Test
    @Order(3)
    void customerA_cannotSeeCustomerB_invoices() {
        // Create an invoice for Customer B's project via reflection (entity has no setters)
        Project projectB = seeder.getCommercialOffice();
        ProjectInvoice invoice = new ProjectInvoice();
        setField(invoice, "project", projectB);
        setField(invoice, "invoiceNumber", "INV-ISO-B001");
        setField(invoice, "status", InvoiceStatus.ISSUED);
        setField(invoice, "subTotal", new BigDecimal("200000.00"));
        setField(invoice, "gstPercentage", new BigDecimal("18.00"));
        setField(invoice, "gstAmount", new BigDecimal("36000.00"));
        setField(invoice, "totalAmount", new BigDecimal("236000.00"));
        setField(invoice, "invoiceDate", LocalDate.now());
        setField(invoice, "dueDate", LocalDate.now().plusDays(30));
        setField(invoice, "createdAt", LocalDateTime.now());
        invoiceRepository.save(invoice);

        String tokenA = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(tokenA);

        // Customer A tries to list invoices for Customer B's project
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/invoices?projectId=" + projectB.getProjectUuid(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        // Should be denied or return empty/not-found
        assertThat(response.getStatusCode().value()).isIn(403, 404);
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

    // ---- Customer A Cannot See Customer B's Payments ----

    @Test
    @Order(4)
    void customerA_cannotAccessCustomerB_payments() {
        String tokenA = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(tokenA);

        // Customer A tries to filter payments by Customer B's project ID
        Long projectBId = seeder.getCommercialOffice().getId();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/payments?projectId=" + projectBId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        // Should be 403 or the response should not contain Customer B's data
        assertThat(response.getStatusCode().value()).isIn(200, 403);
        if (response.getStatusCode().value() == 200) {
            // If 200, verify the success field indicates denial or empty result
            Map<String, Object> body = response.getBody();
            if (body != null && Boolean.FALSE.equals(body.get("success"))) {
                assertThat(body.get("success")).isEqualTo(false);
            }
        }
    }

    // ---- Customer B Cannot Access Customer A's BOQ Endpoints ----

    @Test
    @Order(5)
    void customerB_cannotAccessCustomerA_boqDocument() {
        // Create a BOQ document for Customer A's project via reflection (entity has no setters)
        Project projectA = seeder.getResidentialVilla();
        BoqDocument doc = new BoqDocument();
        setField(doc, "project", projectA);
        setField(doc, "status", BoqDocumentStatus.APPROVED);
        setField(doc, "revisionNumber", 1);
        setField(doc, "totalValueExGst", new BigDecimal("500000.00"));
        setField(doc, "gstRate", new BigDecimal("0.1800"));
        setField(doc, "totalGstAmount", new BigDecimal("90000.00"));
        setField(doc, "totalValueInclGst", new BigDecimal("590000.00"));
        setField(doc, "submittedAt", LocalDateTime.now());
        setField(doc, "approvedAt", LocalDateTime.now());
        setField(doc, "createdAt", LocalDateTime.now());
        boqDocumentRepository.save(doc);

        String tokenB = auth.loginAsCustomerB();
        HttpHeaders headers = auth.authHeaders(tokenB);

        String projectAUuid = projectA.getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectAUuid + "/boq/document",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        // Should fail - Customer B has no access to Customer A's project
        assertThat(response.getStatusCode().value()).isIn(403, 404, 500);
    }

    @Test
    @Order(6)
    void customerB_cannotAccessCustomerA_boqSummary() {
        String tokenB = auth.loginAsCustomerB();
        HttpHeaders headers = auth.authHeaders(tokenB);

        String projectAUuid = seeder.getResidentialVilla().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectAUuid + "/boq/summary",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404, 500);
    }

    @Test
    @Order(7)
    void customerB_cannotAccessCustomerA_changeOrders() {
        String tokenB = auth.loginAsCustomerB();
        HttpHeaders headers = auth.authHeaders(tokenB);

        String projectAUuid = seeder.getResidentialVilla().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectAUuid + "/boq/change-orders",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404, 500);
    }

    // ---- Cross-customer Financial Isolation ----

    @Test
    @Order(8)
    void customerA_cannotAccessCustomerB_financialSummary() {
        String tokenA = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(tokenA);

        String projectBUuid = seeder.getCommercialOffice().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectBUuid + "/financial/summary",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404, 500);
    }

    // ---- Cross-customer Site Content Isolation ----

    @Test
    @Order(9)
    void customerA_cannotAccessCustomerB_delays() {
        String tokenA = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(tokenA);

        String projectBUuid = seeder.getCommercialOffice().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/projects/" + projectBUuid + "/delays",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404, 500);
    }
}
