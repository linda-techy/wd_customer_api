package com.wd.custapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.service.CrAccessGuard;
import com.wd.custapi.service.PortalApiClient;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class CustomerCrApprovalControllerTest extends TestcontainersPostgresBase {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @MockitoBean PortalApiClient portalApiClient;
    @MockitoBean CustomerUserRepository customerUserRepository;
    @MockitoBean CrAccessGuard crAccessGuard;

    @BeforeEach
    void setUp() {
        CustomerUser u = new CustomerUser();
        u.setId(7L);
        u.setEmail("ravi@example.com");
        when(customerUserRepository.findByEmail("ravi@example.com"))
            .thenReturn(Optional.of(u));
    }

    @Test
    @WithMockUser(username = "ravi@example.com")
    void requestOtpReturns202OnSuccess() throws Exception {
        when(portalApiClient.requestCrOtp(42L, 7L))
            .thenReturn(Map.of("status", "SENT"));

        mvc.perform(post("/api/customer/cr/42/request-otp").with(csrf()))
            .andExpect(status().isAccepted());
    }

    @Test
    @WithMockUser(username = "ravi@example.com")
    void requestOtpReturns429WhenRateLimited() throws Exception {
        when(portalApiClient.requestCrOtp(42L, 7L))
            .thenThrow(new PortalApiClient.RateLimitedException(3600));

        mvc.perform(post("/api/customer/cr/42/request-otp").with(csrf()))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.retryAfterSeconds").value(3600));
    }

    @Test
    void requestOtpRejectsAnonymous() throws Exception {
        // Customer-API SecurityConfig rejects unauthenticated requests with 403 Forbidden
        // (its access-denied handler does not differentiate 401 from 403; mirrors
        // FileDownloadIdorIntegrationTest convention).
        mvc.perform(post("/api/customer/cr/42/request-otp").with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "ravi@example.com")
    void approveReturns200WithVerifiedStatusOnSuccess() throws Exception {
        when(portalApiClient.approveCr(eq(42L), eq(7L), eq("123456"), anyString()))
            .thenReturn(Map.of("status", "VERIFIED"));

        mvc.perform(post("/api/customer/cr/42/approve").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "9.9.9.9, 1.1.1.1")
                .content(json.writeValueAsString(Map.of("otpCode", "123456"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("VERIFIED"));

        verify(portalApiClient).approveCr(42L, 7L, "123456", "9.9.9.9");
    }

    @Test
    @WithMockUser(username = "ravi@example.com")
    void approveReturns400WithWrongCodeStatusWhenPortalRejects() throws Exception {
        when(portalApiClient.approveCr(any(), any(), any(), any()))
            .thenReturn(Map.of("status", "WRONG_CODE"));

        mvc.perform(post("/api/customer/cr/42/approve").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("otpCode", "999999"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("WRONG_CODE"));
    }

    @Test
    @WithMockUser(username = "ravi@example.com")
    void rateLimitedResponseIncludesRetryAfterAndErrorCode() throws Exception {
        when(portalApiClient.requestCrOtp(42L, 7L))
            .thenThrow(new PortalApiClient.RateLimitedException(1800));

        mvc.perform(post("/api/customer/cr/42/request-otp").with(csrf()))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("RATE_LIMITED"))
            .andExpect(jsonPath("$.retryAfterSeconds").value(1800));
    }

    @Test
    @WithMockUser(username = "ravi@example.com")
    void requestOtpReturns403WhenCustomerDoesNotOwnCrProject() throws Exception {
        // Customer A trying to request OTP for customer B's CR — guard throws.
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException(
                "Access denied: CR not in customer's project"))
            .when(crAccessGuard).assertCustomerCanAccessCr(7L, "ravi@example.com", 42L);

        mvc.perform(post("/api/customer/cr/42/request-otp").with(csrf()))
            .andExpect(status().isForbidden());

        // The portal-API must NOT be invoked when ownership check fails.
        org.mockito.Mockito.verifyNoInteractions(portalApiClient);
    }

    @Test
    @WithMockUser(username = "ravi@example.com")
    void approveReturns403WhenCustomerDoesNotOwnCrProject() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.security.access.AccessDeniedException(
                "Access denied: CR not in customer's project"))
            .when(crAccessGuard).assertCustomerCanAccessCr(7L, "ravi@example.com", 42L);

        mvc.perform(post("/api/customer/cr/42/approve").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("otpCode", "123456"))))
            .andExpect(status().isForbidden());

        org.mockito.Mockito.verifyNoInteractions(portalApiClient);
    }
}
