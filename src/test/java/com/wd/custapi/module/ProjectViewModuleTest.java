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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Dashboard / Project view endpoints (/api/dashboard/*).
 * Covers dashboard overview, recent projects, search, project details, and phases.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectViewModuleTest extends TestcontainersPostgresBase {

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

    // ---- Dashboard Overview ----

    @Test
    @Order(1)
    void getDashboard_authenticated_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(2)
    void getDashboard_unauthenticated_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        // Spring Security returns 403 for unauthenticated requests when no httpBasic/formLogin is configured
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    // ---- Recent Projects ----

    @Test
    @Order(3)
    void getRecentProjects_authenticated_returnsList() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/recent-projects",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        // Customer A has 1 project (Residential Villa)
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(1);
    }

    // ---- Search Projects ----

    @Test
    @Order(4)
    void searchProjects_withQuery_returnMatchingProjects() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/search-projects?q=Residential",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(5)
    void searchProjects_emptyQuery_returnsRecentProjects() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/search-projects",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---- Project Details ----

    @Test
    @Order(6)
    @SuppressWarnings("unchecked")
    void getProjectDetails_ownProject_returnsDetails() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);
        String projectUuid = seeder.getResidentialVilla().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/projects/" + projectUuid,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(7)
    void getProjectDetails_otherCustomerProject_returnsForbiddenOrNotFound() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);
        // Customer A tries to access Customer B's project
        String projectUuid = seeder.getCommercialOffice().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/projects/" + projectUuid,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        // Should be 403 or 404 (access denied or project not found for this customer)
        assertThat(response.getStatusCode().value()).isIn(403, 404);
    }

    // ---- Project Phases ----

    @Test
    @Order(8)
    void getProjectPhases_ownProject_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);
        String projectUuid = seeder.getResidentialVilla().getProjectUuid().toString();

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/projects/" + projectUuid + "/phases",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // May be empty if no phases seeded, but endpoint should respond OK
    }

    @Test
    @Order(9)
    void getProjectPhases_otherCustomerProject_returnsForbiddenOrNotFound() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);
        String projectUuid = seeder.getCommercialOffice().getProjectUuid().toString();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/dashboard/projects/" + projectUuid + "/phases",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode().value()).isIn(403, 404);
    }
}
