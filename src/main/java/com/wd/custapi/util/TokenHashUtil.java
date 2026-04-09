package com.wd.custapi.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for hashing high-entropy tokens (JWT refresh tokens) before DB storage.
 *
 * <p>SHA-256 is suitable here because JWT tokens are already high-entropy random strings
 * (256-bit HMAC). We only need protection against DB-dump harvest — not brute force —
 * so a fast, unsalted hash is acceptable. KDFs (bcrypt/argon2) are unnecessary overhead.
 *
 * <p>Returns a 64-character lowercase hex string.
 */
public final class TokenHashUtil {

    private TokenHashUtil() {
        // Utility class — no instances
    }

    /**
     * Returns the SHA-256 hash of {@code rawToken} as a 64-character lowercase hex string.
     *
     * @param rawToken the plaintext token (e.g. a raw refresh JWT)
     * @return 64-char hex digest
     * @throws IllegalStateException if SHA-256 is unavailable (never happens on any JVM 8+)
     */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
