package com.wd.custapi.security;

import com.wd.custapi.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("Authorization header is null or doesn't start with Bearer. Skipping JWT filter.");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        // Validate token first
        if (!jwtService.validateToken(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token type and subject
        String tokenType = jwtService.extractTokenType(jwt);
        String actualSubject = jwtService.extractActualSubject(jwt);

        // Handle different token types
        if ("CUSTOMER".equals(tokenType)) {
            // Customer user authentication
            handleCustomerAuthentication(jwt, actualSubject, request);
        } else {
            // Default customer user authentication
            handleCustomerAuthentication(jwt, actualSubject, request);
        }

        filterChain.doFilter(request, response);
    }

    private void handleCustomerAuthentication(String jwt, String email, HttpServletRequest request) {
        try {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);

            if (jwtService.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("Successfully authenticated user: {} with authorities: {}",
                        email, userDetails.getAuthorities());
            }
        } catch (Exception e) {
            // If user not found in customer users, skip authentication
            // This allows the request to proceed to the controller where it will be
            // rejected
        }
    }
}
