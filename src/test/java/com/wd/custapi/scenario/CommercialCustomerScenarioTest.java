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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end scenario test for Customer B's commercial project journey.
 *
 * Simulates a commercial customer checking their project: login, dashboard,
 * project details, financial overview, invoices, payments, delay logs,
 * and quality checks. Commercial projects tend to be finance-heavy, so
 * this scenario emphasizes the financial and compliance endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommercialCustomerScenarioTest extends TestcontainersPostgresBase {

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

    // ---- Step 1: Login as Customer B ----

    @Test
    @Order(1)
    void step01_loginAsCustomerB() {
        token = auth.loginAsCustomerB();
        assertThat(token).isNotBlank();

        projectUuid = seeder.getCommercialOffice().getProjectUuid().toString();
    }

    // ---- Step 2: Dashboard loads ----

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

    // ---- Step 3: Commercial project details ----

    @Test
    @Order(3)
    void step03_getProjectDetails() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/projects/" + projectUuid,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    // ---- Step 4: Financial summary ----

    @Test
    @Order(4)
    void step04_getFinancialSummary() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/financial/summary",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Financial summary should contain aggregate payment fields
        assertThat(response.getBody()).containsKey("totalStages");
        assertThat(response.getBody()).containsKey("totalPaidToDate");
    }

    // ---- Step 5: Invoices ----

    @Test
    @Order(5)
    void step05_getInvoices() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/invoices",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("success");
    }

    // ---- Step 6: Payments ----

    @Test
    @Order(6)
    void step06_getPayments() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/payments",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("success");
    }

    // ---- Step 7: Delay logs ----

    @Test
    @Order(7)
    void step07_getDelayLogs() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/projects/" + projectUuid + "/delays",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("delays");
        assertThat(response.getBody()).containsKey("count");
    }

    // ---- Step 8: Quality checks ----

    @Test
    @Order(8)
    void step08_getQualityChecks() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/quality-check",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("success");
    }
}
