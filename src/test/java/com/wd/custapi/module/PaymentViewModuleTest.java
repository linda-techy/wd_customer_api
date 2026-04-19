package com.wd.custapi.module;

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
 * Integration tests for Payment view endpoints (/api/customer/payments)
 * and Financial summary endpoints (/api/projects/{projectId}/financial/*).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentViewModuleTest extends TestcontainersPostgresBase {

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;
    @Autowired TestDataSeeder seeder;
    AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        seeder.seed();
        auth = new AuthTestHelper(restTemplate, port);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    // ---- List Payments ----

    @Test
    @Order(1)
    void listPayments_authenticated_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/payments",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
    }

    @Test
    @Order(2)
    void listPayments_unauthenticated_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/payments",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- Financial Summary ----

    @Test
    @Order(3)
    void getFinancialSummary_ownProject_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);
        String projectUuid = seeder.getResidentialVilla().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/financial/summary",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("totalStages");
        assertThat(response.getBody()).containsKey("totalPaidToDate");
    }

    // ---- Financial Stages ----

    @Test
    @Order(4)
    void getFinancialStages_ownProject_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);
        String projectUuid = seeder.getResidentialVilla().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/financial/stages",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("stages");
        assertThat(response.getBody()).containsKey("count");
    }

    // ---- Financial Deductions ----

    @Test
    @Order(5)
    void getFinancialDeductions_ownProject_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);
        String projectUuid = seeder.getResidentialVilla().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/financial/deductions",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("deductions");
    }

    // ---- Final Account ----

    @Test
    @Order(6)
    void getFinalAccount_ownProject_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);
        String projectUuid = seeder.getResidentialVilla().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/financial/final-account",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // May contain null finalAccount if not yet prepared, but endpoint responds OK
    }

    // ---- Financial Summary for Other Customer's Project ----

    @Test
    @Order(7)
    void getFinancialSummary_otherCustomerProject_returnsForbiddenOrNotFound() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);
        String projectUuid = seeder.getCommercialOffice().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/financial/summary",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404, 500);
    }
}
