package com.wd.custapi.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.algorithm:HS256}")
    private String algorithm;

    @Value("${jwt.private-key:}")
    private String privateKeyPem;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${jwt.aud.value}")
    private String audValue;

    @Value("${jwt.aud.enforce}")
    private boolean audEnforce;

    /**
     * Always-initialized HMAC key — used for signing in HS256 mode and for
     * backward-compatible verification of old tokens when running in RS256 mode.
     */
    private SecretKey hmacKey;

    /**
     * Active signing key — either the RSA private key (RS256 mode) or the HMAC
     * key (HS256 mode). Set in {@link #initSigningKey()}.
     */
    private Key signingKey;

    /** RSA public key derived from the private key — used for RS256 verification. */
    private java.security.PublicKey rsaPublicKey;

    /** True when RS256 is configured and a valid private key was loaded. */
    private boolean useRsa = false;

    @PostConstruct
    private void initSigningKey() {
        // Always build the HMAC key — needed for backward-compat fallback verification.
        this.hmacKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        if ("RS256".equalsIgnoreCase(algorithm) && privateKeyPem != null && !privateKeyPem.isBlank()) {
            java.security.PrivateKey privateKey = loadRsaPrivateKey(privateKeyPem);
            this.signingKey = privateKey;
            this.rsaPublicKey = derivePublicKey(privateKey);
            this.useRsa = true;
            logger.info("JWT signing initialized with RS256 (RSA)");
        } else {
            this.signingKey = hmacKey;
            this.useRsa = false;
            logger.info("JWT signing initialized with HS256 (HMAC)");
        }
    }

    private Key getSigningKey() {
        return signingKey; // Zero-allocation — cached at startup
    }

    // ── RSA helpers ──────────────────────────────────────────────────────────

    private java.security.PrivateKey loadRsaPrivateKey(String pem) {
        try {
            String keyContent = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] keyBytes = java.util.Base64.getDecoder().decode(keyContent);
            java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
            return java.security.KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key", e);
        }
    }

    private java.security.PublicKey derivePublicKey(java.security.PrivateKey privateKey) {
        try {
            java.security.interfaces.RSAPrivateCrtKey crtKey = (java.security.interfaces.RSAPrivateCrtKey) privateKey;
            java.security.spec.RSAPublicKeySpec publicSpec = new java.security.spec.RSAPublicKeySpec(
                    crtKey.getModulus(), crtKey.getPublicExponent());
            return java.security.KeyFactory.getInstance("RSA").generatePublic(publicSpec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive RSA public key from private key", e);
        }
    }

    // ── Claims extraction ────────────────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse and verify a JWT.
     * <p>
     * When running in RS256 mode: tries the RSA public key first, then falls back
     * to the HMAC key so that tokens issued before the migration are still accepted
     * during the transition window.
     * <p>
     * When running in HS256 mode: verifies with the HMAC key only.
     */
    private Claims extractAllClaims(String token) {
        if (useRsa) {
            try {
                return Jwts.parser()
                        .verifyWith(rsaPublicKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (io.jsonwebtoken.security.SignatureException e) {
                // RS256 signature check failed — token may have been signed with the old
                // HS256 key before migration. Try HMAC as a backward-compat fallback.
                logger.debug("RS256 verification failed, attempting HS256 fallback for backward compatibility");
                return Jwts.parser()
                        .verifyWith(hmacKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            }
        }
        return Jwts.parser()
                .verifyWith(hmacKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Token generation ─────────────────────────────────────────────────────

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "CUSTOMER"); // signed claim — not guessable via subject prefix
        return createToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "REFRESH");
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    // Multi-tenant token generation — tokenType stored as a SIGNED claim, not subject prefix
    public String generateToken(String subject, String tokenType, Map<String, Object> claims, Long expiration) {
        claims = new HashMap<>(claims); // defensive copy
        claims.put("tokenType", tokenType); // cryptographically signed in JWT payload
        return createToken(claims, subject, expiration); // subject = just the email, no prefix
    }

    public String generateCustomerToken(String email, Map<String, Object> claims) {
        return generateToken(email, "CUSTOMER", claims, accessTokenExpiration);
    }

    /**
     * Extract token type from the signed "tokenType" claim.
     * Previously read from subject prefix ("CUSTOMER_email") — easily guessable/forgeable.
     * Now reads from a cryptographically-signed claim in the JWT payload.
     * Backward-compatible: if claim is missing, returns "CUSTOMER" as safe default.
     */
    public String extractTokenType(String token) {
        Object tokenType = extractAllClaims(token).get("tokenType");
        if (tokenType != null) {
            return tokenType.toString();
        }
        return "CUSTOMER"; // safe default for legacy tokens issued before this fix
    }

    /**
     * Extract the actual email subject.
     * Previously had to strip the "CUSTOMER_" prefix — subject is now just the email.
     * Backward-compatible: strips legacy prefix for tokens issued before this fix.
     */
    public String extractActualSubject(String token) {
        String subject = extractUsername(token);
        if (subject != null && subject.contains("_")) {
            // Legacy token: subject had prefix e.g. "CUSTOMER_user@email.com"
            return subject.substring(subject.indexOf("_") + 1);
        }
        return subject;
    }

    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        JwtBuilder builder = Jwts.builder()
                .claims(claims)
                .subject(subject)
                .audience().add(audValue).and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration));
        // RS256 and HS256 are different JJWT types — must branch rather than ternary
        if (useRsa) {
            builder = builder.signWith((java.security.PrivateKey) getSigningKey(), Jwts.SIG.RS256);
        } else {
            builder = builder.signWith(hmacKey, Jwts.SIG.HS256);
        }
        return builder.compact();
    }

    // ── Validation ───────────────────────────────────────────────────────────

    public Boolean validateToken(String token, UserDetails userDetails) {
        if (!validateToken(token)) {
            return false;
        }
        final String actualSubject = extractActualSubject(token);
        return actualSubject.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public Boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);

            java.util.Set<String> tokenAud = claims.getAudience();
            boolean audMatches = tokenAud != null && tokenAud.contains(audValue);

            if (!audMatches) {
                if (audEnforce) {
                    return false;
                }
                logger.warn("JWT missing or mismatched aud claim (token audience={}, expected={}). "
                        + "This should only occur during the phased aud rollout — investigate if seen post-rollout.",
                        tokenAud, audValue);
            }
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
