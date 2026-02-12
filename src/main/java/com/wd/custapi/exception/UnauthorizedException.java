package com.wd.custapi.exception;

/**
 * Exception thrown when a user attempts to access a resource without proper authorization.
 * This exception should result in an HTTP 403 response.
 */
public class UnauthorizedException extends RuntimeException {
    private final String action;
    private final String resource;

    public UnauthorizedException(String action, String resource) {
        super(String.format("Not authorized to %s %s", action, resource));
        this.action = action;
        this.resource = resource;
    }

    public UnauthorizedException(String message) {
        super(message);
        this.action = "access";
        this.resource = "resource";
    }

    public String getAction() {
        return action;
    }

    public String getResource() {
        return resource;
    }
}
