package com.wd.custapi.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper for authentication in integration tests.
 * <p>
 * Wraps the auth endpoints so test classes can obtain JWT tokens with a single
 * method call. Uses {@link TestRestTemplate} to hit the running server.
 *
 * <pre>
 * AuthTestHelper auth = new AuthTestHelper(restTemplate, port);
 * String token = auth.loginAsCustomerA();
 * HttpHeaders headers = auth.authHeaders(token);
 * </pre>
 */
public class AuthTestHelper {

    private static final String CUSTOMER_A_EMAIL = "customerA@test.com";
    private static final String CUSTOMER_B_EMAIL = "customerB@test.com";
    private static final String DEFAULT_PASSWORD = "password123";

    private final TestRestTemplate restTemplate;
    private final int port;

    public AuthTestHelper(TestRestTemplate restTemplate, int port) {
        this.restTemplate = restTemplate;
        this.port = port;
    }

    /**
     * Logs in as Customer A (customerA@test.com) and returns the access token.
     */
    public String loginAsCustomerA() {
        return login(CUSTOMER_A_EMAIL, DEFAULT_PASSWORD);
    }

    /**
     * Logs in as Customer B (customerB@test.com) and returns the access token.
     */
    public String loginAsCustomerB() {
        return login(CUSTOMER_B_EMAIL, DEFAULT_PASSWORD);
    }

    /**
     * Posts to /auth/login with the given credentials and returns the access token.
     *
     * @param email    user email
     * @param password user password
     * @return JWT access token
     * @throws RuntimeException if login fails
     */
    @SuppressWarnings("unchecked")
    public String login(String email, String password) {
        String url = baseUrl() + "/auth/login";

        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Login failed for " + email
                    + ": status=" + response.getStatusCode());
        }

        Object accessToken = response.getBody().get("accessToken");
        if (accessToken == null) {
            throw new RuntimeException("Login response missing 'accessToken' for " + email);
        }
        return accessToken.toString();
    }

    /**
     * Posts to /auth/register with the given details and returns the access token.
     *
     * @param firstName first name
     * @param lastName  last name
     * @param email     user email
     * @param password  user password
     * @return JWT access token
     * @throws RuntimeException if registration fails
     */
    @SuppressWarnings("unchecked")
    public String register(String firstName, String lastName, String email, String password) {
        String url = baseUrl() + "/auth/register";

        Map<String, String> body = new LinkedHashMap<>();
        body.put("firstName", firstName);
        body.put("lastName", lastName);
        body.put("email", email);
        body.put("password", password);
        body.put("phone", "9000000001");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Registration failed for " + email
                    + ": status=" + response.getStatusCode());
        }

        Object accessToken = response.getBody().get("accessToken");
        if (accessToken == null) {
            throw new RuntimeException("Register response missing 'accessToken' for " + email);
        }
        return accessToken.toString();
    }

    /**
     * Creates {@link HttpHeaders} with a Bearer authorization token.
     *
     * @param token JWT access token
     * @return headers ready to use in authenticated requests
     */
    public HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String baseUrl() {
        return "http://localhost:" + port;
    }
}
