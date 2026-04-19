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
 * End-to-end scenario test for Customer C's renovation project journey.
 *
 * Simulates a renovation customer reviewing their project: login, dashboard,
 * project details, BOQ documents, change orders (full list and pending review),
 * observations, and warranties. Renovation projects commonly involve scope
 * changes, so this scenario emphasizes the change order and defect workflows.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RenovationCustomerScenarioTest extends TestcontainersPostgresBase {

    private static final String CUSTOMER_C_EMAIL = "customerC@test.com";
    private static final String DEFAULT_PASSWORD = "password123";

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
        AuthTestHelper.clearTokenCache();
    }

    @BeforeEach
    void setUp() {
        auth = new AuthTestHelper(restTemplate, port);
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }

    // ---- Step 1: Login as Customer C ----

    @Test
    @Order(1)
    void step01_loginAsCustomerC() {
        // AuthTestHelper does not have a loginAsCustomerC shortcut, so use login() directly
        token = auth.login(CUSTOMER_C_EMAIL, DEFAULT_PASSWORD);
        assertThat(token).isNotBlank();

        projectUuid = seeder.getRenovationHome().getProjectUuid().toString();
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

    // ---- Step 3: Renovation project details ----

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

    // ---- Step 4: BOQ document ----

    @Test
    @Order(4)
    void step04_getBoqDocument() {
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

    // ---- Step 5: Change orders list ----

    @Test
    @Order(5)
    void step05_getChangeOrders() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/boq/change-orders",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("changeOrders");
    }

    // ---- Step 6: Pending change orders for review ----

    @Test
    @Order(6)
    void step06_getPendingChangeOrders() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/boq/change-orders/pending-review",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("changeOrders");
    }

    // ---- Step 7: Observations ----

    @Test
    @Order(7)
    void step07_getObservations() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuid + "/observations",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("success");
    }

    // ---- Step 8: Warranties ----

    @Test
    @Order(8)
    void step08_getWarranties() {
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/projects/" + projectUuid + "/warranties",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("warranties");
        assertThat(response.getBody()).containsKey("count");
    }
}
