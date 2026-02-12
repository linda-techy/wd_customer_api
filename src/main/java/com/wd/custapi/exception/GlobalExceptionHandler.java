package com.wd.custapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler to catch all unhandled exceptions
 * and provide detailed error responses
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle IO exceptions (file access issues)
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex, WebRequest request) {
        System.err.println("=== IOException Caught ===");
        System.err.println("Message: " + ex.getMessage());
        System.err.println("Request: " + request.getDescription(false));
        ex.printStackTrace();
        System.err.println("========================");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "File Access Error");
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("path", request.getDescription(false));
        errorDetails.put("type", ex.getClass().getSimpleName());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }

    /**
     * Handle file access denied exceptions
     */
    @ExceptionHandler(java.nio.file.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleFileAccessDeniedException(
            java.nio.file.AccessDeniedException ex, WebRequest request) {
        System.err.println("=== File AccessDeniedException Caught ===");
        System.err.println("File: " + ex.getFile());
        System.err.println("Message: " + ex.getMessage());
        System.err.println("Request: " + request.getDescription(false));
        ex.printStackTrace();
        System.err.println("========================================");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "File Permission Denied");
        errorDetails.put("message", "Cannot read file: " + ex.getMessage());
        errorDetails.put("file", ex.getFile());
        errorDetails.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDetails);
    }

    /**
     * Handle Spring Security access denied
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        System.err.println("=== Security AccessDeniedException Caught ===");
        System.err.println("Message: " + ex.getMessage());
        System.err.println("Request: " + request.getDescription(false));
        ex.printStackTrace();
        System.err.println("===========================================");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Access Denied");
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDetails);
    }

    /**
     * Handle Spring MVC Resource Not Found (404)
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, WebRequest request) {
        logger.warn("Resource not found: {} {}", ex.getHttpMethod(), ex.getResourcePath());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Not Found");
        errorDetails.put("message", "Resource not found: " + ex.getResourcePath());
        errorDetails.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails);
    }

    /**
     * Handle Validation Exceptions (400)
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            org.springframework.web.bind.MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Validation Failed");
        errorDetails.put("message", "Input validation failed");
        errorDetails.put("details", errors);
        errorDetails.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    /**
     * Handle Missing Request Parameter (400)
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(
            org.springframework.web.bind.MissingServletRequestParameterException ex, WebRequest request) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Bad Request");
        errorDetails.put("message", "Missing required parameter: " + ex.getParameterName());
        errorDetails.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    /**
     * Handle Method Not Supported (405)
     */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest request) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Method Not Allowed");
        errorDetails.put("message", "HTTP method " + ex.getMethod() + " is not supported for this endpoint");
        errorDetails.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorDetails);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, WebRequest request) {
        logger.error("UNHANDLED EXCEPTION: {} - {}", ex.getClass().getName(), ex.getMessage(), ex);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", "An unexpected error occurred");
        errorDetails.put("type", ex.getClass().getSimpleName());
        errorDetails.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }

    /**
     * Handle NullPointerException
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(
            NullPointerException ex, WebRequest request) {
        System.err.println("=== NullPointerException Caught ===");
        System.err.println("Request: " + request.getDescription(false));
        System.err.println("Stack Trace:");
        ex.printStackTrace();
        System.err.println("===================================");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Null Pointer Error");
        errorDetails.put("message", "A null value was encountered");
        errorDetails.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        System.err.println("=== IllegalArgumentException Caught ===");
        System.err.println("Message: " + ex.getMessage());
        System.err.println("Request: " + request.getDescription(false));
        ex.printStackTrace();
        System.err.println("======================================");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Invalid Argument");
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("path", request.getDescription(false));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }
}
