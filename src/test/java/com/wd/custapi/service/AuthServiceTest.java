package com.wd.custapi.service;

import com.wd.custapi.dto.LoginRequest;
import com.wd.custapi.dto.LoginResponse;
import com.wd.custapi.dto.RefreshTokenRequest;
import com.wd.custapi.dto.RefreshTokenResponse;
import com.wd.custapi.dto.RegisterRequest;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.RefreshToken;
import com.wd.custapi.model.Role;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.EmailVerificationTokenRepository;
import com.wd.custapi.repository.PasswordResetTokenRepository;
import com.wd.custapi.repository.RefreshTokenRepository;
import com.wd.custapi.repository.RoleRepository;
import com.wd.custapi.util.TokenHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomerUserRepository customerUserRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private CustomerUser user;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "customerPortalBaseUrl", "https://cust.walldotbuilders.com");

        customerRole = new Role();
        customerRole.setName("CUSTOMER");

        user = new CustomerUser();
        user.setId(1L);
        user.setEmail("john@example.com");
        user.setPassword("hashed-password");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(customerRole);
        user.setEnabled(true);
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokenPair() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        LoginRequest request = new LoginRequest("john@example.com", "Password@1");
        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertNotNull(response.getUser());
        assertEquals("john@example.com", response.getUser().getEmail());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_invalidPassword_throwsBadCredentialsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest request = new LoginRequest("john@example.com", "wrong-password");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_nonexistentEmail_throwsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("User not found"));

        LoginRequest request = new LoginRequest("unknown@example.com", "Password@1");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_newEmail_createsUserWithHashedPassword() {
        when(customerUserRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("Secret@1")).thenReturn("hashed-secret");
        when(customerUserRepository.save(any(CustomerUser.class))).thenAnswer(inv -> {
            CustomerUser u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(jwtService.generateAccessToken(any(CustomerUser.class))).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any(CustomerUser.class))).thenReturn("new-refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("Secret@1");
        request.setFirstName("Jane");
        request.setLastName("Smith");

        LoginResponse response = authService.register(request);

        ArgumentCaptor<CustomerUser> captor = ArgumentCaptor.forClass(CustomerUser.class);
        verify(customerUserRepository, atLeastOnce()).save(captor.capture());

        CustomerUser saved = captor.getAllValues().get(0);
        assertEquals("hashed-secret", saved.getPassword());
        assertEquals("new@example.com", saved.getEmail());
        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
    }

    @Test
    void register_duplicateEmail_throwsRuntimeException() {
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));

        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password@1");
        request.setFirstName("John");
        request.setLastName("Doe");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(customerUserRepository, never()).save(any());
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    void changePassword_correctCurrentPassword_succeeds() {
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@1", "hashed-password")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@1")).thenReturn("new-hashed");

        authService.changePassword("john@example.com", "OldPass@1", "NewPass@1");

        assertEquals("new-hashed", user.getPassword());
        verify(customerUserRepository).save(user);
        verify(refreshTokenRepository).deleteByUser_Id(1L);
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsIllegalArgumentException() {
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "hashed-password")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword("john@example.com", "WrongPass", "NewPass@1"));
        verify(customerUserRepository, never()).save(any());
    }

    @Test
    void changePassword_samePassword_throwsIllegalArgumentException() {
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SamePass@1", "hashed-password")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword("john@example.com", "SamePass@1", "SamePass@1"));
        verify(customerUserRepository, never()).save(any());
    }

    @Test
    void changePassword_revokesAllRefreshTokens() {
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@1", "hashed-password")).thenReturn(true);
        when(passwordEncoder.encode("NewPass@2")).thenReturn("another-hash");

        authService.changePassword("john@example.com", "OldPass@1", "NewPass@2");

        verify(refreshTokenRepository).deleteByUser_Id(1L);
    }

    // ── refreshToken ──────────────────────────────────────────────────────────

    @Test
    void refreshToken_validToken_returnsNewPair() {
        String rawToken = "valid-refresh-token";
        String tokenHash = TokenHashUtil.hash(rawToken);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(tokenHash);
        storedToken.setUser(user);
        storedToken.setRevoked(false);
        storedToken.setExpiryDate(LocalDateTime.now().plusDays(7));

        when(jwtService.validateToken(rawToken)).thenReturn(true);
        when(jwtService.extractUsername(rawToken)).thenReturn("john@example.com");
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByToken(tokenHash)).thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);
        RefreshTokenResponse response = authService.refreshToken(request);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        verify(refreshTokenRepository).delete(storedToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_expiredToken_throwsRuntimeException() {
        String rawToken = "expired-refresh-token";
        String tokenHash = TokenHashUtil.hash(rawToken);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(tokenHash);
        storedToken.setUser(user);
        storedToken.setRevoked(false);
        storedToken.setExpiryDate(LocalDateTime.now().minusDays(1)); // expired

        when(jwtService.validateToken(rawToken)).thenReturn(true);
        when(jwtService.extractUsername(rawToken)).thenReturn("john@example.com");
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByToken(tokenHash)).thenReturn(Optional.of(storedToken));

        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertTrue(ex.getMessage().toLowerCase().contains("expired") || ex.getMessage().toLowerCase().contains("revoked"));
    }

    @Test
    void refreshToken_revokedToken_throwsRuntimeException() {
        String rawToken = "revoked-refresh-token";
        String tokenHash = TokenHashUtil.hash(rawToken);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(tokenHash);
        storedToken.setUser(user);
        storedToken.setRevoked(true);
        storedToken.setExpiryDate(LocalDateTime.now().plusDays(7));

        when(jwtService.validateToken(rawToken)).thenReturn(true);
        when(jwtService.extractUsername(rawToken)).thenReturn("john@example.com");
        when(customerUserRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByToken(tokenHash)).thenReturn(Optional.of(storedToken));

        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertTrue(ex.getMessage().toLowerCase().contains("revoked") || ex.getMessage().toLowerCase().contains("expired"));
    }

    @Test
    void refreshToken_invalidSignature_throwsRuntimeException() {
        when(jwtService.validateToken("bad-token")).thenReturn(false);

        RefreshTokenRequest request = new RefreshTokenRequest("bad-token");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.refreshToken(request));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }
}
