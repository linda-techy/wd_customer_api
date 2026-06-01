package com.wd.custapi.module;

import com.wd.custapi.config.TestDataSeeder;
import com.wd.custapi.support.AuthTestHelper;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for site content endpoints.
 * Covers site reports, quality checks, observations, galleries,
 * delay logs, warranties, and CCTV cameras.
 *
 * These endpoints are read-heavy; test data may be empty but endpoints must respond OK.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SiteContentModuleTest extends TestcontainersPostgresBase {

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

    private String projectUuidA() {
        return seeder.getResidentialVilla().getProjectUuid().toString();
    }

    // ---- Site Reports (/api/customer/site-reports) ----

    @Test
    @Order(1)
    void listSiteReports_authenticated_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/site-reports",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
    }

    @Test
    @Order(2)
    void listSiteReports_unauthenticated_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/site-reports",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        // Spring Security returns 403 for unauthenticated requests when no httpBasic/formLogin is configured
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    // ---- Project read endpoints that return a { success: ... } envelope ----
    //   /api/projects/{projectId}/quality-check
    //   /api/projects/{projectId}/observations
    //   /api/projects/{projectId}/gallery

    @ParameterizedTest
    @Order(3)
    @ValueSource(strings = {"quality-check", "observations", "gallery"})
    void listProjectReadEndpoint_authenticated_returnsOk(String pathSegment) {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuidA() + "/" + pathSegment,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("success");
    }

    // ---- Customer read endpoints that return a { <key>: [...], count: N } envelope ----
    //   /api/customer/projects/{projectId}/delays     -> "delays"
    //   /api/customer/projects/{projectId}/warranties -> "warranties"

    @ParameterizedTest
    @Order(6)
    @CsvSource({
            "delays, delays",
            "warranties, warranties"
    })
    void listCustomerCollectionEndpoint_authenticated_returnsOk(String pathSegment, String collectionKey) {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/projects/" + projectUuidA() + "/" + pathSegment,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey(collectionKey);
        assertThat(response.getBody()).containsKey("count");
    }

    // ---- CCTV Cameras (/api/customer/projects/{projectId}/cctv-cameras) ----

    @Test
    @Order(8)
    void listCctvCameras_authenticated_returnsOk() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/projects/" + projectUuidA() + "/cctv-cameras",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("cameras");
        assertThat(response.getBody()).containsKey("count");
    }

    // ---- Unauthorized Access for Site Content ----

    @Test
    @Order(9)
    void qualityChecks_unauthenticated_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/projects/" + projectUuidA() + "/quality-check",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        // Spring Security returns 403 for unauthenticated requests when no httpBasic/formLogin is configured
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }

    @Test
    @Order(10)
    void delayLogs_unauthenticated_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/customer/projects/" + projectUuidA() + "/delays",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        // Spring Security returns 403 for unauthenticated requests when no httpBasic/formLogin is configured
        assertThat(response.getStatusCode().value()).isIn(401, 403);
    }
}
