package com.wd.custapi.service;

import com.wd.custapi.dto.*;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.EmailVerificationToken;
import com.wd.custapi.util.TokenHashUtil;
import com.wd.custapi.model.PasswordResetToken;
import com.wd.custapi.model.RefreshToken;
import com.wd.custapi.model.Role;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.EmailVerificationTokenRepository;
import com.wd.custapi.repository.PasswordResetTokenRepository;
import com.wd.custapi.repository.RefreshTokenRepository;
import com.wd.custapi.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Value("${app.customer-portal-base-url:https://cust.walldotbuilders.com}")
    private String customerPortalBaseUrl;

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

        LoginResponse.UserInfo userInfo = buildUserInfo(user);

        // Get actual project count for the user from the authentication principal
        long projectCount = user.getAuthorities().size(); // placeholder — replace with real repo call if needed
        String redirectUrl = determineRedirectUrl(projectCount);

        return new LoginResponse(accessToken, refreshToken, jwtService.getAccessTokenExpiration(), userInfo,
                permissions, projectCount, redirectUrl);
    }

    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token signature and expiry
        if (!jwtService.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Get user from refresh token
        String userEmail = jwtService.extractUsername(refreshToken);
        CustomerUser user = customerUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Customer user not found"));

        // Check if refresh token exists and is not revoked (compare against stored hash)
        RefreshToken storedToken = refreshTokenRepository.findByToken(TokenHashUtil.hash(refreshToken))
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (storedToken.isExpired() || storedToken.getRevoked()) {
            throw new RuntimeException("Refresh token expired or revoked");
        }

        // TOKEN ROTATION — revoke the old token and issue a new one.
        // Without this, a stolen refresh token can be used indefinitely for 7 days.
        refreshTokenRepository.delete(storedToken);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        saveRefreshToken(user, newRefreshToken);

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user);

        return new RefreshTokenResponse(newAccessToken, jwtService.getAccessTokenExpiration());
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(TokenHashUtil.hash(refreshToken));
    }

    public LoginResponse.UserInfo getCurrentUser(String email) {
        CustomerUser user = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer user not found"));

        return buildUserInfo(user);
    }

    /**
     * Update a customer's own profile (first name, last name, phone, whatsapp).
     * Email and role are not changeable by the customer.
     */
    @Transactional
    public LoginResponse.UserInfo updateProfile(String email, Map<String, String> updates) {
        CustomerUser user = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer user not found"));

        if (updates.containsKey("firstName") && updates.get("firstName") != null) {
            user.setFirstName(updates.get("firstName").trim());
        }
        if (updates.containsKey("lastName") && updates.get("lastName") != null) {
            user.setLastName(updates.get("lastName").trim());
        }
        if (updates.containsKey("phone")) {
            user.setPhone(updates.get("phone"));
        }
        if (updates.containsKey("whatsappNumber")) {
            user.setWhatsappNumber(updates.get("whatsappNumber"));
        }
        if (updates.containsKey("address")) {
            user.setAddress(updates.get("address"));
        }
        if (updates.containsKey("companyName")) {
            user.setCompanyName(updates.get("companyName"));
        }
        if (updates.containsKey("gstNumber")) {
            user.setGstNumber(updates.get("gstNumber"));
        }

        customerUserRepository.save(user);
        log.info("Profile updated for user: {}", email);

        return buildUserInfo(user);
    }

    /**
     * Change password for an already-authenticated user.
     * Verifies the current password, enforces that the new password differs,
     * then re-hashes and saves. All existing refresh tokens are revoked so
     * the user must re-authenticate on other devices.
     */
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        CustomerUser user = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        customerUserRepository.save(user);

        // Revoke all refresh tokens so user must re-authenticate on other devices
        refreshTokenRepository.deleteByUser_Id(user.getId());

        log.info("Password changed for user: {}", email);
    }

    /**
     * Register or update a device FCM token for push notifications.
     * One token per user — overwrites previous token on new device login.
     */
    @Transactional
    public void registerFcmToken(String email, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new IllegalArgumentException("FCM token cannot be empty");
        }
        CustomerUser user = customerUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer user not found"));
        user.setFcmToken(fcmToken);
        customerUserRepository.save(user);
        log.info("FCM token registered for user: {}", email);
    }

    private void saveRefreshToken(CustomerUser user, String refreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(TokenHashUtil.hash(refreshToken)); // Store SHA-256 hash — never the raw JWT
        token.setExpiryDate(LocalDateTime.now().plusDays(7)); // 7 days
        token.setRevoked(false);

        refreshTokenRepository.save(token);
    }

    /**
     * Nightly cleanup: purge all expired or revoked refresh tokens.
     * Without this, the refresh_tokens table grows forever — in a 10k user app it
     * will contain millions of rows within weeks, making findByToken() very slow.
     * Runs at 2:00 AM IST daily. Bulk deletion avoids OOM vs. an entity-by-entity approach.
     */
    @Transactional
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Kolkata")
    public void cleanupExpiredRefreshTokens() {
        try {
            int deleted = refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
            log.info("Nightly refresh token cleanup: deleted {} expired/revoked entries", deleted);
        } catch (Exception e) {
            log.error("Error during nightly refresh token cleanup", e);
        }
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

        // Send email verification
        newUser.setEmailVerified(false);
        customerUserRepository.save(newUser);

        String verificationToken = UUID.randomUUID().toString();
        EmailVerificationToken evToken = new EmailVerificationToken(
                newUser, verificationToken, LocalDateTime.now().plusHours(24));
        emailVerificationTokenRepository.save(evToken);

        String verificationLink = buildVerificationLink(verificationToken);
        emailService.sendVerificationEmail(newUser.getEmail(), newUser.getFirstName(), verificationLink);

        // Auto-login after registration
        String accessToken = jwtService.generateAccessToken(newUser);
        String refreshToken = jwtService.generateRefreshToken(newUser);
        saveRefreshToken(newUser, refreshToken);

        List<String> permissions = newUser.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

        LoginResponse.UserInfo userInfo = buildUserInfo(newUser);

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

    private LoginResponse.UserInfo buildUserInfo(CustomerUser user) {
        return new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole() != null ? user.getRole().getName() : "VIEWER",
                user.getPhone(),
                user.getWhatsappNumber(),
                user.getAddress(),
                user.getCompanyName(),
                user.getGstNumber(),
                user.getCustomerType());
    }

    private String buildVerificationLink(String token) {
        String normalizedBaseUrl = customerPortalBaseUrl == null
                ? ""
                : customerPortalBaseUrl.trim().replaceAll("/+$", "");
        if (normalizedBaseUrl.isEmpty()) {
            normalizedBaseUrl = "https://cust.walldotbuilders.com";
        }
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        return normalizedBaseUrl + "/#/verify-email?token=" + encodedToken;
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

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken evToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (evToken.isUsed()) {
            throw new IllegalStateException("Verification token has already been used");
        }
        if (evToken.isExpired()) {
            throw new IllegalStateException("Verification token has expired. Please request a new one.");
        }

        evToken.setUsed(true);
        emailVerificationTokenRepository.save(evToken);

        CustomerUser user = evToken.getUser();
        user.setEmailVerified(true);
        customerUserRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());
    }

}
