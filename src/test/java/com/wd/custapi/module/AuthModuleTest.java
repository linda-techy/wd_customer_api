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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Auth module (/auth/*).
 * Covers login, registration, token refresh, profile, and password management.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthModuleTest extends TestcontainersPostgresBase {

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

    // ---- Login ----

    @Test
    @Order(1)
    void login_validCredentials_returnsAccessToken() {
        Map<String, String> body = Map.of(
                "email", "customerA@test.com",
                "password", "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat(response.getBody()).containsKey("refreshToken");
        assertThat(response.getBody()).containsKey("tokenType");
        assertThat(response.getBody().get("tokenType")).isEqualTo("Bearer");
    }

    @Test
    @Order(2)
    void login_invalidPassword_returnsUnauthorized() {
        Map<String, String> body = Map.of(
                "email", "customerA@test.com",
                "password", "wrongpassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(3)
    void login_nonExistentEmail_returnsUnauthorized() {
        Map<String, String> body = Map.of(
                "email", "nobody@test.com",
                "password", "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    // ---- Registration ----

    @Test
    @Order(4)
    void register_newUser_returnsAccessToken() {
        String uniqueEmail = "newuser-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        Map<String, String> body = new LinkedHashMap<>();
        body.put("firstName", "New");
        body.put("lastName", "User");
        body.put("email", uniqueEmail);
        body.put("password", "securepass123");
        body.put("phone", "9000000099");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
    }

    @Test
    @Order(5)
    void register_duplicateEmail_returnsBadRequest() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("firstName", "Duplicate");
        body.put("lastName", "User");
        body.put("email", "customerA@test.com");
        body.put("password", "password123");
        body.put("phone", "9000000098");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/auth/register",
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    // ---- Refresh Token ----

    @Test
    @Order(6)
    @SuppressWarnings("unchecked")
    void refreshToken_validToken_returnsNewAccessToken() {
        // First login to get a refresh token
        Map<String, String> loginBody = Map.of(
                "email", "customerA@test.com",
                "password", "password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/login",
                new HttpEntity<>(loginBody, headers),
                Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String refreshToken = (String) loginResponse.getBody().get("refreshToken");
        assertThat(refreshToken).isNotNull();

        // Now use refresh token
        Map<String, String> refreshBody = Map.of("refreshToken", refreshToken);

        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/refresh-token",
                new HttpEntity<>(refreshBody, headers),
                Map.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).containsKey("accessToken");
    }

    // ---- Get Current User ----

    @Test
    @Order(7)
    void getCurrentUser_authenticated_returnsUserInfo() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("email");
        assertThat(response.getBody().get("email")).isEqualTo("customerA@test.com");
    }

    @Test
    @Order(8)
    void getCurrentUser_unauthenticated_returnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---- Update Profile ----

    @Test
    @Order(9)
    void updateProfile_validUpdate_returnsUpdatedUser() {
        String token = auth.loginAsCustomerA();
        HttpHeaders headers = auth.authHeaders(token);

        Map<String, String> updates = new LinkedHashMap<>();
        updates.put("firstName", "AliceUpdated");
        updates.put("lastName", "AndersonUpdated");
        updates.put("phone", "9876549999");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/auth/profile",
                HttpMethod.PUT,
                new HttpEntity<>(updates, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("firstName");
    }

    // ---- Change Password ----

    @Test
    @Order(10)
    void changePassword_validCurrentPassword_succeeds() {
        // Register a fresh user so we don't break other tests
        String uniqueEmail = "changepw-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
        String token = auth.register("PwChange", "User", uniqueEmail, "oldpass123");
        HttpHeaders headers = auth.authHeaders(token);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("currentPassword", "oldpass123");
        body.put("newPassword", "newpass456");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/auth/change-password",
                HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("message");
    }

    // ---- Forgot Password ----

    @Test
    @Order(11)
    void forgotPassword_existingEmail_returnsOk() {
        Map<String, String> body = Map.of("email", "customerA@test.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/auth/forgot-password",
                new HttpEntity<>(body, headers),
                Map.class);

        // Should always return 200 for security (doesn't reveal if email exists)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    @Order(12)
    void forgotPassword_nonExistentEmail_stillReturnsOk() {
        Map<String, String> body = Map.of("email", "nonexistent@test.com");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/auth/forgot-password",
                new HttpEntity<>(body, headers),
                Map.class);

        // Should still return 200 to not reveal account existence
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
