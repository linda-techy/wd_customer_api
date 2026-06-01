package com.wd.custapi.exception;

/**
 * Generic unchecked application exception for the customer API.
 *
 * <p>Replaces raw {@code throw new RuntimeException(...)} (SonarQube java:S112).
 * Intentionally has NO dedicated {@code @ExceptionHandler}: like a plain
 * {@link RuntimeException} it falls through to the catch-all
 * {@code @ExceptionHandler(Exception.class)} in {@link GlobalExceptionHandler}
 * (HTTP 500, generic message), so runtime behaviour is unchanged.
 */
public class CustomerApiException extends RuntimeException {

    public CustomerApiException(String message) {
        super(message);
    }

    public CustomerApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
