package com.wd.custapi.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;

/**
 * Filter to log all incoming requests and responses
 * Especially useful for debugging 500 errors that don't reach controllers
 */
@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Only log storage-related requests
        String requestURI = httpRequest.getRequestURI();
        if (requestURI != null && requestURI.contains("/api/storage")) {
            System.out.println("\n\n");
            System.out.println("################################################");
            System.out.println("### FILTER: INCOMING REQUEST");
            System.out.println("################################################");
            System.out.println("Timestamp: " + java.time.LocalDateTime.now());
            System.out.println("Method: " + httpRequest.getMethod());
            System.out.println("URI: " + requestURI);
            System.out.println("Query String: " + httpRequest.getQueryString());
            System.out.println("Remote Address: " + httpRequest.getRemoteAddr());
            System.out.println("Auth Present: " + (httpRequest.getHeader("Authorization") != null));
            
            // Log headers
            System.out.println("Headers:");
            Enumeration<String> headerNames = httpRequest.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = httpRequest.getHeader(headerName);
                // Don't log full Authorization token for security
                if (headerName.equalsIgnoreCase("Authorization")) {
                    headerValue = headerValue != null && headerValue.length() > 20 
                        ? headerValue.substring(0, 20) + "..." 
                        : headerValue;
                }
                System.out.println("  " + headerName + ": " + headerValue);
            }
            System.out.println("################################################");
            System.out.println("\n");
        }
        
        try {
            // Continue with the request
            chain.doFilter(request, response);
            
            // Log response status
            if (requestURI != null && requestURI.contains("/api/storage")) {
                System.out.println("\n");
                System.out.println("################################################");
                System.out.println("### FILTER: RESPONSE");
                System.out.println("################################################");
                System.out.println("Timestamp: " + java.time.LocalDateTime.now());
                System.out.println("URI: " + requestURI);
                System.out.println("Status: " + httpResponse.getStatus());
                System.out.println("Content Type: " + httpResponse.getContentType());
                System.out.println("################################################");
                System.out.println("\n\n");
            }
            
        } catch (Exception e) {
            System.err.println("\n\n");
            System.err.println("################################################");
            System.err.println("### FILTER: EXCEPTION CAUGHT");
            System.err.println("################################################");
            System.err.println("Timestamp: " + java.time.LocalDateTime.now());
            System.err.println("URI: " + requestURI);
            System.err.println("Exception Type: " + e.getClass().getName());
            System.err.println("Exception Message: " + e.getMessage());
            System.err.println("Stack Trace:");
            e.printStackTrace();
            System.err.println("################################################");
            System.err.println("\n\n");
            
            // Re-throw to let Spring handle it
            throw e;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("RequestLoggingFilter initialized");
    }

    @Override
    public void destroy() {
        System.out.println("RequestLoggingFilter destroyed");
    }
}

