package com.wd.custapi.exception;

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
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, WebRequest request) {
        System.err.println("=== UNHANDLED EXCEPTION ===");
        System.err.println("Type: " + ex.getClass().getName());
        System.err.println("Message: " + ex.getMessage());
        System.err.println("Request: " + request.getDescription(false));
        System.err.println("Stack Trace:");
        ex.printStackTrace();
        System.err.println("===========================");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "Internal Server Error");
        errorDetails.put("message", ex.getMessage());
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
