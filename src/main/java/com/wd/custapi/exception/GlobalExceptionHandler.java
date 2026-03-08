package com.wd.custapi.exception;

import com.wd.custapi.dto.ApiError;
import com.wd.custapi.logging.LoggingConstants;
import com.wd.custapi.logging.SensitiveDataMasker;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private String getTraceId() {
        String traceId = MDC.get(LoggingConstants.MDC_TRACE_ID);
        return traceId != null ? traceId : ("FALLBACK-" + UUID.randomUUID().toString().substring(0, 8));
    }

    private String resolveUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "anonymous";
    }

    private void logWarn(String message, Exception ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String userId = resolveUserId();
        String path = request.getRequestURI();
        String safeMsg = SensitiveDataMasker.mask(ex.getMessage() != null ? ex.getMessage() : "null");
        logger.warn("[{}] User:{} Path:{} - {}: {}", traceId, userId, path, message, safeMsg);
    }

    private void logError(String message, Exception ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String userId = resolveUserId();
        String path = request.getRequestURI();
        
        // Put values in MDC so the JSON encoder outputs them in structured logs
        MDC.put(LoggingConstants.MDC_USER_ID, userId);
        MDC.put("requestPath", path);
        try {
            logger.error("[{}] User:{} Path:{} - {}", traceId, userId, path, message, ex);
        } finally {
            MDC.remove("requestPath");
            // TraceIdFilter handles cleanup of traceId and userId
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        logWarn("Resource not found", ex, request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ApiError(ex.getMessage(), "RESOURCE_NOT_FOUND", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        logWarn("Unauthorized (" + ex.getAction() + " on " + ex.getResource() + ")", ex, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            new ApiError("Access denied", "ACCESS_DENIED", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        logWarn("Business rule violation", ex, request);
        return ResponseEntity.status(ex.getStatus()).body(
            new ApiError(ex.getMessage(), ex.getErrorCode(), getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiError> handleIOException(IOException ex, HttpServletRequest request) {
        logError("File IO error", ex, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiError("File access error occurred", "FILE_ACCESS_ERROR", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(java.nio.file.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleFileAccessDeniedException(java.nio.file.AccessDeniedException ex, HttpServletRequest request) {
        logError("File access denied", ex, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            new ApiError("File permission denied", "FILE_PERMISSION_DENIED", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        logWarn("Spring Security access denied", ex, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            new ApiError("Access denied", "ACCESS_DENIED", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        logWarn("No handler found", ex, request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ApiError("Resource not found: " + ex.getResourcePath(), "RESOURCE_NOT_FOUND", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex, HttpServletRequest request) {
        logWarn("Input validation failed", ex, request);
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("success", false);
        errorDetails.put("message", "Input validation failed");
        errorDetails.put("errorCode", "VALIDATION_ERROR");
        errorDetails.put("correlationId", getTraceId());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("timestamp", System.currentTimeMillis());
        errorDetails.put("validationErrors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDetails);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParams(org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest request) {
        logWarn("Missing parameter: " + ex.getParameterName(), ex, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ApiError("Missing required parameter: " + ex.getParameterName(), "MISSING_PARAMETER", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        logWarn("HTTP Method not supported: " + ex.getMethod(), ex, request);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
            new ApiError("HTTP method " + ex.getMethod() + " not supported", "METHOD_NOT_ALLOWED", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiError> handleNullPointerException(NullPointerException ex, HttpServletRequest request) {
        logError("Null pointer exception", ex, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiError("An unexpected error occurred", "NULL_POINTER_ERROR", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        logWarn("Illegal argument", ex, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ApiError(ex.getMessage(), "INVALID_ARGUMENT", getTraceId(), request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGlobalException(Exception ex, HttpServletRequest request) {
        logError("Unhandled exception", ex, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiError("An unexpected error occurred", "INTERNAL_ERROR", getTraceId(), request.getRequestURI()));
    }
}
