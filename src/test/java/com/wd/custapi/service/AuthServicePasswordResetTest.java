package com.wd.custapi.service;

import com.wd.custapi.dto.ForgotPasswordRequest;
import com.wd.custapi.dto.ResetPasswordRequest;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.PasswordResetToken;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.PasswordResetTokenRepository;
import com.wd.custapi.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordResetTest {

    @Mock
    private CustomerUserRepository customerUserRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "customerPortalBaseUrl", "https://app.walldotbuilders.com");
    }

    @Test
    void forgotPassword_unknownEmail_doesNotSendOrPersistToken() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");

        when(customerUserRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(request, "127.0.0.1");

        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    void forgotPassword_knownEmail_storesHashAndSendsRawTokenLink() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("known@example.com");

        CustomerUser user = new CustomerUser();
        user.setFirstName("Known");
        when(customerUserRepository.findByEmail("known@example.com")).thenReturn(Optional.of(user));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        authService.forgotPassword(request, "127.0.0.1");

        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        verify(emailService).sendPasswordResetEmail(eq("known@example.com"), eq("Known"), linkCaptor.capture());

        PasswordResetToken saved = tokenCaptor.getValue();
        String resetLink = linkCaptor.getValue();
        URI uri = URI.create(resetLink);
        String rawToken = extractQueryParam(uri, "token");

        assertEquals("known@example.com", saved.getEmail());
        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());
        assertTrue(saved.getResetCode().matches("^[a-f0-9]{64}$"));
        assertNotEquals(rawToken, saved.getResetCode());
        assertEquals("/reset-password", uri.getPath());
        assertEquals("known@example.com", extractQueryParam(uri, "email"));
    }

    @Test
    void resetPassword_validToken_updatesPasswordAndRevokesSessions() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("known@example.com");
        request.setResetCode("plain-reset-token");
        request.setNewPassword("NewPass@123");

        String tokenHash = sha256Hex(request.getResetCode());
        PasswordResetToken token = new PasswordResetToken();
        token.setId(42L);
        token.setEmail("known@example.com");
        token.setResetCode(tokenHash);
        token.setUsed(false);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        CustomerUser user = new CustomerUser();
        user.setId(99L);

        when(passwordResetTokenRepository.findByEmailAndResetCodeAndUsedFalse("known@example.com", tokenHash))
                .thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.markTokenUsedIfUnused(42L)).thenReturn(1);
        when(customerUserRepository.findByEmail("known@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encoded-password");

        authService.resetPassword(request, "127.0.0.1");

        assertEquals("encoded-password", user.getPassword());
        verify(customerUserRepository).save(user);
        verify(refreshTokenRepository).deleteByUser_Id(99L);
    }

    private static String extractQueryParam(URI uri, String key) {
        if (uri.getQuery() == null) return null;
        String[] pairs = uri.getQuery().split("&");
        for (String pair : pairs) {
            String[] split = pair.split("=", 2);
            if (split.length == 2 && split[0].equals(key)) {
                return java.net.URLDecoder.decode(split[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
