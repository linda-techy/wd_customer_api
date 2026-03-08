package com.wd.custapi.logging;

import java.util.regex.Pattern;

/**
 * Utility class for masking sensitive data in log messages.
 * Prevents passwords, tokens, secrets, and OTPs from appearing in log files.
 *
 * Example input:  {"password":"mySecret123","email":"user@test.com"}
 * Example output: {"password":"****","email":"user@test.com"}
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {}

    private static final String MASK = "****";

    // Matches JSON-style: "fieldName":"value" or "fieldName": "value"
    private static final Pattern[] JSON_PATTERNS = buildJsonPatterns();

    // Matches query-param / form style: fieldName=value
    private static final Pattern[] PARAM_PATTERNS = buildParamPatterns();

    private static Pattern[] buildJsonPatterns() {
        return java.util.Arrays.stream(LoggingConstants.SENSITIVE_FIELDS)
                .map(field -> Pattern.compile(
                        "(?i)(\"" + Pattern.quote(field) + "\"\\s*:\\s*\")([^\"]*)(\")",
                        Pattern.CASE_INSENSITIVE))
                .toArray(Pattern[]::new);
    }

    private static Pattern[] buildParamPatterns() {
        return java.util.Arrays.stream(LoggingConstants.SENSITIVE_FIELDS)
                .map(field -> Pattern.compile(
                        "(?i)(" + Pattern.quote(field) + "=)([^&\\s,}]+)",
                        Pattern.CASE_INSENSITIVE))
                .toArray(Pattern[]::new);
    }

    /**
     * Mask sensitive fields in any string (JSON body, query params, log messages).
     */
    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;

        for (Pattern p : JSON_PATTERNS) {
            result = p.matcher(result).replaceAll("$1" + MASK + "$3");
        }
        for (Pattern p : PARAM_PATTERNS) {
            result = p.matcher(result).replaceAll("$1" + MASK);
        }
        return result;
    }

    /** Convenience — mask a header value to show only scheme prefix */
    public static String maskAuthHeader(String headerValue) {
        if (headerValue == null) return null;
        if (headerValue.toLowerCase().startsWith("bearer ")) {
            return "Bearer ****";
        }
        return MASK;
    }
}
