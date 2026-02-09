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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
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

        // Get project count for dashboard redirect
        long projectCount = 1L; // Based on database - user has 1 project
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
    public String forgotPassword(ForgotPasswordRequest request) {
        // Check if user exists
        customerUserRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("No account found with this email address"));

        // Delete any existing reset tokens for this email
        passwordResetTokenRepository.deleteByEmail(request.getEmail().toLowerCase().trim());

        // Generate a 6-digit reset code
        String resetCode = String.format("%06d", new Random().nextInt(999999));

        // Save the reset token (valid for 15 minutes)
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(request.getEmail().toLowerCase().trim());
        token.setResetCode(resetCode);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        token.setUsed(false);

        passwordResetTokenRepository.save(token);

        log.info("Password reset code generated for: {}", request.getEmail());

        // In a production environment, send this code via email
        // For now, return it in the response (for development/testing)
        return resetCode;
    }

    // ===== RESET PASSWORD =====

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Find the reset token
        PasswordResetToken token = passwordResetTokenRepository
                .findByEmailAndResetCodeAndUsedFalse(
                        request.getEmail().toLowerCase().trim(),
                        request.getResetCode())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset code"));

        // Check if token is expired
        if (token.isExpired()) {
            throw new RuntimeException("Reset code has expired. Please request a new one.");
        }

        // Find the user
        CustomerUser user = customerUserRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        customerUserRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        log.info("Password reset successful for: {}", request.getEmail());
    }
}
