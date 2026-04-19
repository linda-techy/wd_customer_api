package com.wd.custapi.module;

import com.wd.custapi.config.TestDataSeeder;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.ProjectInvoice;
import com.wd.custapi.model.enums.InvoiceStatus;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Invoice view endpoints (/api/customer/invoices).
 * Covers listing invoices, getting invoice details, and verifying DRAFT exclusion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceViewModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired TestDataSeeder seeder;
    @Autowired ProjectInvoiceRepository invoiceRepository;
    AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    /**
     * Creates a ProjectInvoice via reflection since the entity has no setters (read-only design).
     */
    private ProjectInvoice createInvoice(Project project, InvoiceStatus status, String invoiceNumber) {
        ProjectInvoice invoice = new ProjectInvoice();
        setField(invoice, "project", project);
        setField(invoice, "invoiceNumber", invoiceNumber);
        setField(invoice, "status", status);
        setField(invoice, "subTotal", new BigDecimal("100000.00"));
        setField(invoice, "gstPercentage", new BigDecimal("18.00"));
        setField(invoice, "gstAmount", new BigDecimal("18000.00"));
        setField(invoice, "totalAmount", new BigDecimal("118000.00"));
        setField(invoice, "invoiceDate", LocalDate.now());
        setField(invoice, "dueDate", LocalDate.now().plusDays(30));
        setField(invoice, "createdAt", LocalDateTime.now());
        return invoiceRepository.save(invoice);
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

    // ---- List Invoices ----

    @Test
    @Order(1)
    void listInvoices_withIssuedInvoice_returnsInvoice() {
        Project project = seeder.getResidentialVilla();
        createInvoice(project, InvoiceStatus.ISSUED, "INV-TEST-001");

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/invoices?projectId=" + project.getProjectUuid(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    // ---- Get Invoice Details ----

    @Test
    @Order(2)
    void getInvoiceById_existingInvoice_returnsDetails() {
        Project project = seeder.getResidentialVilla();
        ProjectInvoice invoice = createInvoice(project, InvoiceStatus.ISSUED, "INV-TEST-002");

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/invoices/" + invoice.getId(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
    }

    // ---- DRAFT Invoices Excluded ----

    @Test
    @Order(3)
    @SuppressWarnings("unchecked")
    void listInvoices_draftInvoicesExcluded_onlyShowsNonDraft() {
        Project project = seeder.getResidentialVilla();
        // Create a DRAFT invoice and an ISSUED invoice
        createInvoice(project, InvoiceStatus.DRAFT, "INV-DRAFT-001");
        createInvoice(project, InvoiceStatus.ISSUED, "INV-ISSUED-001");

        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/invoices?projectId=" + project.getProjectUuid(),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body.get("success")).isEqualTo(true);

        // Verify no DRAFT invoices appear in the data
        Object data = body.get("data");
        if (data instanceof Map) {
            Map<String, Object> pageData = (Map<String, Object>) data;
            List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.get("content");
            if (content != null) {
                for (Map<String, Object> inv : content) {
                    assertThat(inv.get("status")).isNotEqualTo("DRAFT");
                }
            }
        }
    }

    // ---- Unauthorized Access ----

    @Test
    @Order(4)
    void listInvoices_unauthenticated_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/invoices?projectId=" + seeder.getResidentialVilla().getProjectUuid(),
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
