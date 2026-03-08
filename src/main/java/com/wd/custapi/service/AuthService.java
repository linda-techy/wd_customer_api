package com.wd.custapi.service;

import com.wd.custapi.dto.*;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.PasswordResetToken;
import com.wd.custapi.model.RefreshToken;
import com.wd.custapi.model.Role;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.PasswordResetTokenRepository;
import com.wd.custapi.repository.RefreshTokenRepository;
import com.wd.custapi.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${app.customer-portal-base-url:https://cust.walldotbuilders.com}")
    private String customerPortalBaseUrl;

    private static final int FORGOT_PASSWORD_MAX_ATTEMPTS = 5;
    private static final int RESET_PASSWORD_MAX_ATTEMPTS = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_RATE_LIMIT_KEYS = 10_000;
    private static final int MAX_CLIENT_KEY_LENGTH = 128;

    private final ConcurrentHashMap<String, Deque<LocalDateTime>> forgotPasswordAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<LocalDateTime>> resetPasswordAttempts = new ConcurrentHashMap<>();

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()));

        CustomerUser user = (CustomerUser) authentication.getPrincipal();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Save refresh token
        saveRefreshToken(user, refreshToken);

        // Get user permissions
        List<String> permissions = user.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName());

        // Get actual project count for the user from the authentication principal
        long projectCount = user.getAuthorities().size(); // placeholder — replace with real repo call if needed
        String redirectUrl = determineRedirectUrl(projectCount);

        return new LoginResponse(accessToken, refreshToken, jwtService.getAccessTokenExpiration(), userInfo,
                permissions, projectCount, redirectUrl);
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        if (!jwtService.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Get user from refresh token
        String userEmail = jwtService.extractUsername(refreshToken);
        CustomerUser user = customerUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Customer user not found"));

        // Check if refresh token exists and is not revoked
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (storedToken.isExpired() || storedToken.getRevoked()) {
            throw new RuntimeException("Refresh token expired or revoked");
        }

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user);

        return new RefreshTokenResponse(newAccessToken, jwtService.getAccessTokenExpiration());
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    public LoginResponse.UserInfo getCurrentUser(String email) {
        CustomerUser user = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer user not found"));

        return new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().getName());
    }

    private void saveRefreshToken(CustomerUser user, String refreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(refreshToken);
        token.setExpiryDate(LocalDateTime.now().plusDays(7)); // 7 days
        token.setRevoked(false);

        refreshTokenRepository.save(token);
    }

    private String determineRedirectUrl(long projectCount) {
        if (projectCount == 0) {
            return "/dashboard"; // No projects, go to dashboard (empty state)
        } else if (projectCount == 1) {
            return "/dashboard"; // Single project, go to dashboard (will show the project)
        } else {
            return "/dashboard"; // Multiple projects, go to dashboard (will show project list)
        }
    }

    // ===== REGISTRATION =====

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // Check if email already exists
        if (customerUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("An account with this email already exists");
        }

        // Get default customer role
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Default customer role not found"));

        // Create new user
        CustomerUser newUser = new CustomerUser();
        newUser.setEmail(request.getEmail().toLowerCase().trim());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setFirstName(request.getFirstName().trim());
        newUser.setLastName(request.getLastName().trim());
        newUser.setRole(customerRole);
        newUser.setEnabled(true);

        customerUserRepository.save(newUser);
        log.info("New customer registered: {}", newUser.getEmail());

        // Auto-login after registration
        String accessToken = jwtService.generateAccessToken(newUser);
        String refreshToken = jwtService.generateRefreshToken(newUser);
        saveRefreshToken(newUser, refreshToken);

        List<String> permissions = newUser.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                newUser.getId(),
                newUser.getEmail(),
                newUser.getFirstName(),
                newUser.getLastName(),
                newUser.getRole().getName());

        return new LoginResponse(accessToken, refreshToken, jwtService.getAccessTokenExpiration(), userInfo,
                permissions, 0L, "/dashboard");
    }

    // ===== FORGOT PASSWORD =====

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        forgotPassword(request, "unknown-client");
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, String clientKey) {
        long startedAtMs = System.currentTimeMillis();
        String email = request.getEmail().toLowerCase().trim();
        String normalizedClientKey = normalizeClientKey(clientKey);
        enforceRateLimit(
                forgotPasswordAttempts,
                "forgot:" + normalizedClientKey + ":" + email,
                FORGOT_PASSWORD_MAX_ATTEMPTS,
                RATE_LIMIT_WINDOW);

        // Check if user exists — silently succeed if not found to prevent email enumeration
        CustomerUser user = customerUserRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.info("Password reset requested for unknown email.");
            log.info("Forgot password completed in {} ms", System.currentTimeMillis() - startedAtMs);
            return;
        }

        // Delete any existing reset tokens for this email
        passwordResetTokenRepository.deleteByEmail(email);

        // Generate high-entropy raw token for URL, but store only its hash.
        String resetToken = UUID.randomUUID() + "." + UUID.randomUUID();
        String resetTokenHash = hashToken(resetToken);

        // Save the reset token (valid for 15 minutes)
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setResetCode(resetTokenHash);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        token.setUsed(false);

        passwordResetTokenRepository.save(token);

        // Build the deep-link reset URL pointing at the customer portal
        String resetLink = buildResetLink(resetToken, email);

        log.info("Password reset link generated for: {}", email);

        // Send the reset email (async — falls back to log simulation when email is disabled)
        emailService.sendPasswordResetEmail(email, user.getFirstName(), resetLink);
        log.info("Forgot password completed in {} ms", System.currentTimeMillis() - startedAtMs);
    }

    // ===== RESET PASSWORD =====

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        resetPassword(request, "unknown-client");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String clientKey) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        String normalizedClientKey = normalizeClientKey(clientKey);
        enforceRateLimit(
                resetPasswordAttempts,
                "reset:" + normalizedClientKey + ":" + normalizedEmail,
                RESET_PASSWORD_MAX_ATTEMPTS,
                RATE_LIMIT_WINDOW);

        String resetCode = request.getResetCode().trim();
        if (resetCode.isEmpty()) {
            throw new IllegalArgumentException("Reset code is required");
        }
        String resetTokenHash = hashToken(resetCode);

        // Find the reset token
        PasswordResetToken token = passwordResetTokenRepository
                .findByEmailAndResetCodeAndUsedFalse(
                        normalizedEmail,
                        resetTokenHash)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset code"));

        // Check if token is expired
        if (token.isExpired()) {
            throw new RuntimeException("Reset code has expired. Please request a new one.");
        }

        int consumedRows = passwordResetTokenRepository.markTokenUsedIfUnused(token.getId());
        if (consumedRows == 0) {
            throw new RuntimeException("Reset code has already been used. Please request a new one.");
        }

        // Find the user
        CustomerUser user = customerUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        customerUserRepository.save(user);

        // Revoke existing sessions after password reset.
        refreshTokenRepository.deleteByUser_Id(user.getId());

        log.info("Password reset successful.");
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String buildResetLink(String resetToken, String email) {
        String normalizedBaseUrl = customerPortalBaseUrl == null
                ? ""
                : customerPortalBaseUrl.trim().replaceAll("/+$", "");
        if (normalizedBaseUrl.isEmpty()) {
            normalizedBaseUrl = "https://app.walldotbuilders.com";
        }
        String encodedToken = URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
        return normalizedBaseUrl
                + "/#/reset_password?token=" + encodedToken
                + "&email=" + encodedEmail;
    }

    private String normalizeClientKey(String rawClientKey) {
        if (rawClientKey == null) {
            return "unknown-client";
        }
        String trimmed = rawClientKey.trim();
        if (trimmed.isEmpty()) {
            return "unknown-client";
        }
        String sanitized = trimmed.replaceAll("[^A-Za-z0-9:._\\-]", "_");
        if (sanitized.length() > MAX_CLIENT_KEY_LENGTH) {
            sanitized = sanitized.substring(0, MAX_CLIENT_KEY_LENGTH);
        }
        return sanitized;
    }

    private void enforceRateLimit(
            ConcurrentHashMap<String, Deque<LocalDateTime>> store,
            String key,
            int maxAttempts,
            Duration window) {
        LocalDateTime now = LocalDateTime.now();
        cleanupRateLimitStore(store, now, window);
        Deque<LocalDateTime> attempts = store.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (attempts) {
            LocalDateTime cutoff = now.minus(window);
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                attempts.pollFirst();
            }

            if (attempts.size() >= maxAttempts) {
                throw new IllegalArgumentException("Too many requests. Please try again later.");
            }

            attempts.addLast(now);
        }
    }

    private void cleanupRateLimitStore(
            ConcurrentHashMap<String, Deque<LocalDateTime>> store,
            LocalDateTime now,
            Duration window) {
        if (store.size() <= MAX_RATE_LIMIT_KEYS) {
            return;
        }

        LocalDateTime cutoff = now.minus(window);
        Iterator<Map.Entry<String, Deque<LocalDateTime>>> iterator = store.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Deque<LocalDateTime>> entry = iterator.next();
            Deque<LocalDateTime> attempts = entry.getValue();
            synchronized (attempts) {
                while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
                    attempts.pollFirst();
                }
                if (attempts.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }
}
