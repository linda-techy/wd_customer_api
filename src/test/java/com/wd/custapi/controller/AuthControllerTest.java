package com.wd.custapi.controller;

import com.wd.custapi.dto.ChangePasswordRequest;
import com.wd.custapi.dto.ForgotPasswordRequest;
import com.wd.custapi.dto.LoginRequest;
import com.wd.custapi.dto.LoginResponse;
import com.wd.custapi.dto.RefreshTokenRequest;
import com.wd.custapi.dto.RefreshTokenResponse;
import com.wd.custapi.dto.RegisterRequest;
import com.wd.custapi.dto.ResetPasswordRequest;
import com.wd.custapi.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit tests for {@link AuthController}. No Spring context, no
 * MockMvc, no DB — the controller is constructed via {@link InjectMocks} with a
 * mocked {@link AuthService}, methods are invoked directly, and the returned
 * {@link ResponseEntity} (status + body) is asserted.
 *
 * <p>Covers every public endpoint plus the dev-profile debug-detail branch and
 * the SecurityContext-backed /me and Authentication-backed mutating endpoints.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    @InjectMocks
    private AuthController controller;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private LoginRequest loginReq(String email) {
        return new LoginRequest(email, "secret123");
    }

    // ---- login ----

    @Test
    void login_success_returns200WithResponse() {
        LoginResponse resp = new LoginResponse();
        when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        ResponseEntity<Object> r = controller.login(loginReq("a@b.com"));

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(resp);
    }

    @Test
    void login_illegalArgument_returns400WithError() {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("bad email"));

        ResponseEntity<Object> r = controller.login(loginReq("a@b.com"));

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody()).isInstanceOf(Map.class);
        assertThat((Map<String, Object>) r.getBody()).containsEntry("error", "bad email");
    }

    @Test
    void login_badCredentials_returns401Generic_whenProdProfile() {
        ReflectionTestUtils.setField(controller, "activeProfile", "prod");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("wrong"));

        ResponseEntity<Object> r = controller.login(loginReq("alice@b.com"));

        assertThat(r.getStatusCode().value()).isEqualTo(401);
        Map<String, Object> body = (Map<String, Object>) r.getBody();
        // prod must NOT leak the failure reason
        assertThat(body)
                .containsEntry("error", "Invalid email or password")
                .doesNotContainKey("debugReason");
    }

    @Test
    void login_userNotFound_returns401WithDebugReason_whenDevProfile() {
        ReflectionTestUtils.setField(controller, "activeProfile", "local");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UsernameNotFoundException("no user"));

        ResponseEntity<Object> r = controller.login(loginReq("alice@b.com"));

        assertThat(r.getStatusCode().value()).isEqualTo(401);
        Map<String, Object> body = (Map<String, Object>) r.getBody();
        assertThat(body)
                .containsEntry("error", "Invalid email or password")
                .containsEntry("debugReason", "USER_NOT_FOUND")
                .containsKey("debugMessage");
    }

    // ---- register ----

    @Test
    void register_success_returns200() {
        LoginResponse resp = new LoginResponse();
        when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

        ResponseEntity<Object> r = controller.register(new RegisterRequest());

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(resp);
    }

    @Test
    void register_illegalArgument_returns400() {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("email taken"));

        ResponseEntity<Object> r = controller.register(new RegisterRequest());

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat((Map<String, Object>) r.getBody()).containsEntry("error", "email taken");
    }

    @Test
    void register_unexpected_returns500Generic() {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("db down"));

        ResponseEntity<Object> r = controller.register(new RegisterRequest());

        assertThat(r.getStatusCode().value()).isEqualTo(500);
        assertThat((Map<String, Object>) r.getBody())
                .containsEntry("error", "Registration failed. Please try again.");
    }

    // ---- forgot-password ----

    private HttpServletRequest reqWithIp(String ip) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn(ip);
        return req;
    }

    @Test
    void forgotPassword_success_returns200WithNeutralMessage() {
        HttpServletRequest req = reqWithIp("1.2.3.4");

        ResponseEntity<Map<String, Object>> r =
                controller.forgotPassword(new ForgotPasswordRequest(), req);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsKey("message");
        verify(authService).forgotPassword(any(ForgotPasswordRequest.class), eq("1.2.3.4"));
    }

    @Test
    void forgotPassword_rateLimited_returns429() {
        HttpServletRequest req = reqWithIp("1.2.3.4");
        doThrow(new IllegalArgumentException("Too many requests"))
                .when(authService).forgotPassword(any(ForgotPasswordRequest.class), anyString());

        ResponseEntity<Map<String, Object>> r =
                controller.forgotPassword(new ForgotPasswordRequest(), req);

        assertThat(r.getStatusCode().value()).isEqualTo(429);
        assertThat(r.getBody()).containsEntry("error", "Too many requests");
    }

    @Test
    void forgotPassword_validationError_returns400() {
        HttpServletRequest req = reqWithIp("1.2.3.4");
        doThrow(new IllegalArgumentException("bad email"))
                .when(authService).forgotPassword(any(ForgotPasswordRequest.class), anyString());

        ResponseEntity<Map<String, Object>> r =
                controller.forgotPassword(new ForgotPasswordRequest(), req);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void forgotPassword_usesXForwardedForFirstHop() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("9.9.9.9, 10.0.0.1");

        controller.forgotPassword(new ForgotPasswordRequest(), req);

        verify(authService).forgotPassword(any(ForgotPasswordRequest.class), eq("9.9.9.9"));
    }

    @Test
    void forgotPassword_unexpected_returns500() {
        HttpServletRequest req = reqWithIp("1.2.3.4");
        doThrow(new RuntimeException("smtp down"))
                .when(authService).forgotPassword(any(ForgotPasswordRequest.class), anyString());

        ResponseEntity<Map<String, Object>> r =
                controller.forgotPassword(new ForgotPasswordRequest(), req);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }

    // ---- reset-password ----

    @Test
    void resetPassword_success_returns200() {
        HttpServletRequest req = reqWithIp("1.2.3.4");

        ResponseEntity<Map<String, Object>> r =
                controller.resetPassword(new ResetPasswordRequest(), req);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).containsKey("message");
    }

    @Test
    void resetPassword_rateLimited_returns429() {
        HttpServletRequest req = reqWithIp("1.2.3.4");
        doThrow(new IllegalArgumentException("Too many attempts"))
                .when(authService).resetPassword(any(ResetPasswordRequest.class), anyString());

        ResponseEntity<Map<String, Object>> r =
                controller.resetPassword(new ResetPasswordRequest(), req);

        assertThat(r.getStatusCode().value()).isEqualTo(429);
    }

    @Test
    void resetPassword_runtimeException_returns400() {
        HttpServletRequest req = reqWithIp("1.2.3.4");
        doThrow(new RuntimeException("token expired"))
                .when(authService).resetPassword(any(ResetPasswordRequest.class), anyString());

        ResponseEntity<Map<String, Object>> r =
                controller.resetPassword(new ResetPasswordRequest(), req);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody()).containsEntry("error", "token expired");
    }

    // ---- refresh-token ----

    @Test
    void refreshToken_success_returns200() {
        RefreshTokenResponse resp = new RefreshTokenResponse("newAccess", 3600L);
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(resp);

        ResponseEntity<Object> r = controller.refreshToken(new RefreshTokenRequest("rt"));

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(resp);
    }

    @Test
    void refreshToken_illegalArgument_returns400() {
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new IllegalArgumentException("missing token"));

        ResponseEntity<Object> r = controller.refreshToken(new RefreshTokenRequest("rt"));

        assertThat(r.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void refreshToken_invalid_returns401() {
        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new RuntimeException("revoked"));

        ResponseEntity<Object> r = controller.refreshToken(new RefreshTokenRequest("rt"));

        assertThat(r.getStatusCode().value()).isEqualTo(401);
        assertThat((Map<String, Object>) r.getBody()).containsEntry("error", "Invalid or expired refresh token");
    }

    // ---- logout ----

    @Test
    void logout_success_returns200() {
        ResponseEntity<Void> r = controller.logout(new RefreshTokenRequest("rt"));

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        verify(authService).logout("rt");
    }

    @Test
    void logout_failure_returns400() {
        doThrow(new RuntimeException("boom")).when(authService).logout(anyString());

        ResponseEntity<Void> r = controller.logout(new RefreshTokenRequest("rt"));

        assertThat(r.getStatusCode().value()).isEqualTo(400);
    }

    // ---- /me ----

    private void setAuthentication(Authentication authentication) {
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(ctx);
    }

    @Test
    void getCurrentUser_authenticated_returns200WithUserInfo() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("userObj");
        when(auth.getName()).thenReturn("alice@b.com");
        setAuthentication(auth);

        LoginResponse.UserInfo info = new LoginResponse.UserInfo(1L, "alice@b.com", "A", "L", "CUSTOMER");
        when(authService.getCurrentUser("alice@b.com")).thenReturn(info);

        ResponseEntity<Object> r = controller.getCurrentUser();

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(info);
    }

    @Test
    void getCurrentUser_noAuthentication_returns401() {
        setAuthentication(null);

        ResponseEntity<Object> r = controller.getCurrentUser();

        assertThat(r.getStatusCode().value()).isEqualTo(401);
        assertThat((Map<String, Object>) r.getBody()).containsEntry("error", "Not authenticated");
    }

    @Test
    void getCurrentUser_anonymous_returns401() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");
        setAuthentication(auth);

        ResponseEntity<Object> r = controller.getCurrentUser();

        assertThat(r.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void getCurrentUser_serviceThrows_returns401AuthFailed() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("userObj");
        when(auth.getName()).thenReturn("alice@b.com");
        setAuthentication(auth);
        when(authService.getCurrentUser(anyString())).thenThrow(new RuntimeException("db"));

        ResponseEntity<Object> r = controller.getCurrentUser();

        assertThat(r.getStatusCode().value()).isEqualTo(401);
        assertThat((Map<String, Object>) r.getBody()).containsEntry("error", "Authentication failed");
    }

    // ---- profile ----

    @Test
    void updateProfile_success_returns200() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("alice@b.com");
        Map<String, String> updates = new HashMap<>();
        updates.put("firstName", "Alice");
        LoginResponse.UserInfo updated = new LoginResponse.UserInfo(1L, "alice@b.com", "Alice", "L", "CUSTOMER");
        when(authService.updateProfile("alice@b.com", updates)).thenReturn(updated);

        ResponseEntity<Object> r = controller.updateProfile(updates, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(r.getBody()).isSameAs(updated);
    }

    @Test
    void updateProfile_nullAuth_returns401() {
        ResponseEntity<Object> r = controller.updateProfile(new HashMap<>(), null);

        assertThat(r.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void updateProfile_illegalArgument_returns400() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("alice@b.com");
        when(authService.updateProfile(anyString(), any()))
                .thenThrow(new IllegalArgumentException("bad phone"));

        ResponseEntity<Object> r = controller.updateProfile(new HashMap<>(), auth);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat((Map<String, Object>) r.getBody()).containsEntry("error", "bad phone");
    }

    @Test
    void updateProfile_unexpected_returns500() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("alice@b.com");
        when(authService.updateProfile(anyString(), any()))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<Object> r = controller.updateProfile(new HashMap<>(), auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }

    // ---- change-password ----

    private ChangePasswordRequest cpr() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("oldPass1");
        req.setNewPassword("newPass12");
        return req;
    }

    @Test
    void changePassword_success_returns200() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("alice@b.com");

        ResponseEntity<Map<String, Object>> r = controller.changePassword(cpr(), auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        verify(authService).changePassword("alice@b.com", "oldPass1", "newPass12");
    }

    @Test
    void changePassword_nullAuth_returns401() {
        ResponseEntity<Map<String, Object>> r = controller.changePassword(cpr(), null);

        assertThat(r.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void changePassword_illegalArgument_returns400() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("alice@b.com");
        doThrow(new IllegalArgumentException("wrong current password"))
                .when(authService).changePassword(anyString(), anyString(), anyString());

        ResponseEntity<Map<String, Object>> r = controller.changePassword(cpr(), auth);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody()).containsEntry("error", "wrong current password");
    }

    @Test
    void changePassword_unexpected_returns500() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("alice@b.com");
        doThrow(new RuntimeException("boom"))
                .when(authService).changePassword(anyString(), anyString(), anyString());

        ResponseEntity<Map<String, Object>> r = controller.changePassword(cpr(), auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }

    // ---- fcm-token ----

    @Test
    void registerFcmToken_success_returns200() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("alice@b.com");
        Map<String, String> body = new HashMap<>();
        body.put("fcmToken", "tok-123");

        ResponseEntity<Map<String, Object>> r = controller.registerFcmToken(body, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        verify(authService).registerFcmToken("alice@b.com", "tok-123");
    }

    @Test
    void registerFcmToken_nullAuth_returns401() {
        ResponseEntity<Map<String, Object>> r = controller.registerFcmToken(new HashMap<>(), null);

        assertThat(r.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void registerFcmToken_blankToken_returns400() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        Map<String, String> body = new HashMap<>();
        body.put("fcmToken", "  ");

        ResponseEntity<Map<String, Object>> r = controller.registerFcmToken(body, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody()).containsEntry("error", "fcmToken is required");
    }

    @Test
    void registerFcmToken_serviceThrows_returns500() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("alice@b.com");
        doThrow(new RuntimeException("fcm err"))
                .when(authService).registerFcmToken(anyString(), anyString());
        Map<String, String> body = new HashMap<>();
        body.put("fcmToken", "tok-123");

        ResponseEntity<Map<String, Object>> r = controller.registerFcmToken(body, auth);

        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }

    // ---- verify-email ----

    @Test
    void verifyEmail_success_returns200() {
        ResponseEntity<Map<String, Object>> r = controller.verifyEmail("vtok");

        assertThat(r.getStatusCode().value()).isEqualTo(200);
        verify(authService).verifyEmail("vtok");
    }

    @Test
    void verifyEmail_illegalState_returns400() {
        doThrow(new IllegalStateException("already verified"))
                .when(authService).verifyEmail(anyString());

        ResponseEntity<Map<String, Object>> r = controller.verifyEmail("vtok");

        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(r.getBody()).containsEntry("error", "already verified");
    }

    @Test
    void verifyEmail_unexpected_returns500() {
        doThrow(new RuntimeException("boom"))
                .when(authService).verifyEmail(anyString());

        ResponseEntity<Map<String, Object>> r = controller.verifyEmail("vtok");

        assertThat(r.getStatusCode().value()).isEqualTo(500);
    }
}
