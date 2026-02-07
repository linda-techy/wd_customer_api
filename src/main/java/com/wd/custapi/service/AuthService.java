package com.wd.custapi.service;

import com.wd.custapi.dto.LoginRequest;
import com.wd.custapi.dto.LoginResponse;
import com.wd.custapi.dto.RefreshTokenRequest;
import com.wd.custapi.dto.RefreshTokenResponse;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.RefreshToken;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomerUserRepository customerUserRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

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
}
