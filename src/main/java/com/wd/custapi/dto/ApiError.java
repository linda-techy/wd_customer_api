package com.wd.custapi.dto;

import java.time.Instant;

/**
 * Standardized error response structure for all API errors.
 * Provides consistent error format with correlation IDs for traceability.
 */
public class ApiError {
    private boolean success = false;
    private String message;
    private String errorCode;
    private String correlationId;
    private Long timestamp;
    private String path;

    public ApiError() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public ApiError(String message) {
        this();
        this.message = message;
    }

    public ApiError(String message, String errorCode) {
        this();
        this.message = message;
        this.errorCode = errorCode;
    }

    public ApiError(String message, String errorCode, String correlationId) {
        this();
        this.message = message;
        this.errorCode = errorCode;
        this.correlationId = correlationId;
    }

    public ApiError(String message, String errorCode, String correlationId, String path) {
        this();
        this.message = message;
        this.errorCode = errorCode;
        this.correlationId = correlationId;
        this.path = path;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
