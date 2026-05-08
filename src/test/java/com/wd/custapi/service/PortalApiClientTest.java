package com.wd.custapi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortalApiClientTest {

    PortalApiClient client;
    RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new PortalApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "portalApiUrl", "http://portal");
        ReflectionTestUtils.setField(client, "sharedSecret", "shh");
    }

    @Test
    void requestCrOtpSendsSignedPostToPortalRequestOtp() {
        when(restTemplate.exchange(eq("http://portal/internal/cr-request-otp"),
                any(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("status", "SENT")));

        Map<String, Object> result = client.requestCrOtp(42L, 7L);
        assertThat(result).containsEntry("status", "SENT");
    }

    @Test
    void requestCrOtpPropagates429AsRateLimitException() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(Map.class)))
            .thenThrow(HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests",
                org.springframework.http.HttpHeaders.EMPTY,
                "{\"error\":\"RATE_LIMITED\",\"retryAfterSeconds\":3600}".getBytes(),
                null));

        assertThatThrownBy(() -> client.requestCrOtp(42L, 7L))
            .isInstanceOf(PortalApiClient.RateLimitedException.class);
    }

    @Test
    void approveCrSendsSignedPostToCrApproveAndReturnsBody() {
        when(restTemplate.exchange(eq("http://portal/internal/cr-approve"),
                any(), any(), eq(Map.class)))
            .thenReturn(ResponseEntity.ok(Map.of("status", "VERIFIED")));

        Map<String, Object> result = client.approveCr(42L, 7L, "123456", "1.2.3.4");
        assertThat(result).containsEntry("status", "VERIFIED");
    }
}
