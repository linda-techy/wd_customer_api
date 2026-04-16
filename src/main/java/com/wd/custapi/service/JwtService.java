package com.wd.custapi.service;

import io.jsonwebtoken.Claims;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${jwt.aud.value}")
    private String audValue;

    @Value("${jwt.aud.enforce}")
    private boolean audEnforce;

    /**
     * Cached signing key — built once at startup, not on every request.
     * Keys.hmacShaKeyFor() is not cheap; caching eliminates hot-path CPU waste.
     */
    private SecretKey signingKey;

    @PostConstruct
    private void initSigningKey() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getSigningKey() {
        return signingKey; // Zero-allocation — cached at startup
    }

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

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

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
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .audience().add(audValue).and()
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        if (!validateToken(token)) {
            return false;
        }
        final String actualSubject = extractActualSubject(token);
        return actualSubject.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public Boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

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
