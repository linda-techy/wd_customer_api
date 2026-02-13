package com.wd.custapi.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;

/**
 * Filter to log all incoming requests and responses
 * Especially useful for debugging 500 errors that don't reach controllers
 */
@Component
public class RequestLoggingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Only log storage-related requests
        String requestURI = httpRequest.getRequestURI();
        if (requestURI != null && requestURI.contains("/api/storage")) {
            logger.debug("\n################################################");
            logger.debug("### FILTER: INCOMING REQUEST");
            logger.debug("################################################");
            logger.debug("Timestamp: {}", java.time.LocalDateTime.now());
            logger.debug("Method: {}", httpRequest.getMethod());
            logger.debug("URI: {}", requestURI);
            logger.debug("Query String: {}", httpRequest.getQueryString());
            logger.debug("Remote Address: {}", httpRequest.getRemoteAddr());
            logger.debug("Auth Present: {}", (httpRequest.getHeader("Authorization") != null));

            // Log headers
            logger.debug("Headers:");
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
                logger.debug("  {}: {}", headerName, headerValue);
            }
            logger.debug("################################################\n");
        }

        try {
            // Continue with the request
            chain.doFilter(request, response);

            // Log response status
            if (requestURI != null && requestURI.contains("/api/storage")) {
                logger.debug("\n################################################");
                logger.debug("### FILTER: RESPONSE");
                logger.debug("################################################");
                logger.debug("Timestamp: {}", java.time.LocalDateTime.now());
                logger.debug("URI: {}", requestURI);
                logger.debug("Status: {}", httpResponse.getStatus());
                logger.debug("Content Type: {}", httpResponse.getContentType());
                logger.debug("################################################\n");
            }

        } catch (Exception e) {
            logger.error("\n################################################");
            logger.error("### FILTER: EXCEPTION CAUGHT");
            logger.error("################################################");
            logger.error("Timestamp: {}", java.time.LocalDateTime.now());
            logger.error("URI: {}", requestURI);
            logger.error("Exception Type: {}", e.getClass().getName());
            logger.error("Exception Message: {}", e.getMessage());
            logger.error("Stack Trace:", e);
            logger.error("################################################\n");

            // Re-throw to let Spring handle it
            throw e;
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("RequestLoggingFilter initialized");
    }

    @Override
    public void destroy() {
        logger.info("RequestLoggingFilter destroyed");
    }
}
