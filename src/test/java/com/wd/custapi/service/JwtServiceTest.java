package com.wd.custapi.service;

import com.wd.custapi.security.JwtConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link JwtService} in HS256 mode. No Spring context — all
 * {@code @Value} fields are injected via {@link ReflectionTestUtils} and the
 * {@code @PostConstruct} {@code initSigningKey()} is invoked manually so the HMAC
 * key is built. Covers generate/extract/validate round-trips plus the expired,
 * bad-signature, malformed, aud-enforce, token-type, and legacy-subject branches.
 */
class JwtServiceTest {

    // 64-byte secret → safe for HS256/384/512 key derivation.
    private static final String SECRET = "test-secret-0123456789-0123456789-0123456789-0123456789-abcdef!!";
    private static final long ACCESS_EXP = 3_600_000L;   // 1h
    private static final long REFRESH_EXP = 86_400_000L; // 24h

    private JwtService jwtService;

    private UserDetails user(String username) {
        return new User(username, "pw", Collections.emptyList());
    }

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "algorithm", "HS256");
        ReflectionTestUtils.setField(jwtService, "privateKeyPem", "");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_EXP);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", REFRESH_EXP);
        ReflectionTestUtils.setField(jwtService, "audValue", JwtConstants.AUDIENCE_CUSTOMER);
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        // Run @PostConstruct manually to build the HMAC signing key.
        ReflectionTestUtils.invokeMethod(jwtService, "initSigningKey");
    }

    // ── generate + extract round-trips ────────────────────────────────────────

    @Test
    void generateAccessToken_thenExtractUsername_roundTrips() {
        String token = jwtService.generateAccessToken(user("alice@test.com"));
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@test.com");
        assertThat(jwtService.extractTokenType(token)).isEqualTo(JwtConstants.TOKEN_TYPE_CUSTOMER);
    }

    @Test
    void generateRefreshToken_carriesRefreshTokenType() {
        String token = jwtService.generateRefreshToken(user("bob@test.com"));
        assertThat(jwtService.extractUsername(token)).isEqualTo("bob@test.com");
        assertThat(jwtService.extractTokenType(token)).isEqualTo("REFRESH");
    }

    @Test
    void generateCustomerToken_carriesCustomerTypeAndExtraClaims() {
        Map<String, Object> extra = new HashMap<>();
        extra.put("userId", 42);
        String token = jwtService.generateCustomerToken("carol@test.com", extra);

        assertThat(jwtService.extractUsername(token)).isEqualTo("carol@test.com");
        assertThat(jwtService.extractTokenType(token)).isEqualTo(JwtConstants.TOKEN_TYPE_CUSTOMER);
        // defensive copy: caller's map is not mutated with tokenType
        assertThat(extra).doesNotContainKey(JwtConstants.CLAIM_TOKEN_TYPE);
    }

    @Test
    void extractExpiration_isInTheFuture() {
        String token = jwtService.generateAccessToken(user("alice@test.com"));
        assertThat(jwtService.extractExpiration(token)).isAfter(new Date());
    }

    // ── validateToken(token) ──────────────────────────────────────────────────

    @Test
    void validateToken_validToken_true() {
        String token = jwtService.generateAccessToken(user("alice@test.com"));
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_garbage_false() {
        assertThat(jwtService.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void validateToken_wrongSignature_false() {
        // Signed with a DIFFERENT secret → signature verification fails.
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-0123456789-0123456789-0123456789-abcdefgh".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder().subject("eve@test.com")
                .audience().add(JwtConstants.AUDIENCE_CUSTOMER).and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey, Jwts.SIG.HS256).compact();
        assertThat(jwtService.validateToken(forged)).isFalse();
    }

    @Test
    void validateToken_missingAud_enforceFalse_true() {
        String token = tokenWithoutAud("alice@test.com");
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        assertThat(jwtService.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_missingAud_enforceTrue_false() {
        String token = tokenWithoutAud("alice@test.com");
        ReflectionTestUtils.setField(jwtService, "audEnforce", true);
        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_wrongAud_enforceTrue_false() {
        String token = tokenWithAud("alice@test.com", JwtConstants.AUDIENCE_PORTAL);
        ReflectionTestUtils.setField(jwtService, "audEnforce", true);
        assertThat(jwtService.validateToken(token)).isFalse();
    }

    // ── validateToken(token, userDetails) ─────────────────────────────────────

    @Test
    void validateTokenWithUser_matchingSubject_true() {
        String token = jwtService.generateAccessToken(user("alice@test.com"));
        assertThat(jwtService.validateToken(token, user("alice@test.com"))).isTrue();
    }

    @Test
    void validateTokenWithUser_mismatchedSubject_false() {
        String token = jwtService.generateAccessToken(user("alice@test.com"));
        assertThat(jwtService.validateToken(token, user("mallory@test.com"))).isFalse();
    }

    @Test
    void validateTokenWithUser_invalidToken_false() {
        assertThat(jwtService.validateToken("garbage", user("alice@test.com"))).isFalse();
    }

    @Test
    void validateTokenWithUser_expiredToken_false() {
        String expired = expiredToken("alice@test.com");
        // aud enforce is false so validateToken(token) passes the aud check, but the
        // expiry check inside validateToken(token, user) must reject it. Note: an
        // expired token throws on parse, so validateToken(token) itself returns false.
        assertThat(jwtService.validateToken(expired, user("alice@test.com"))).isFalse();
    }

    // ── extractTokenType / extractActualSubject branches ──────────────────────

    @Test
    void extractTokenType_legacyTokenWithoutClaim_defaultsToCustomer() {
        String token = tokenWithoutTokenTypeClaim("alice@test.com");
        assertThat(jwtService.extractTokenType(token)).isEqualTo(JwtConstants.DEFAULT_TOKEN_TYPE);
    }

    @Test
    void extractActualSubject_plainEmail_returnedAsIs() {
        String token = jwtService.generateAccessToken(user("alice@test.com"));
        assertThat(jwtService.extractActualSubject(token)).isEqualTo("alice@test.com");
    }

    @Test
    void extractActualSubject_legacyPrefixedSubject_stripsPrefix() {
        String token = tokenWithSubject("CUSTOMER_alice@test.com");
        assertThat(jwtService.extractActualSubject(token)).isEqualTo("alice@test.com");
    }

    @Test
    void getAccessTokenExpiration_returnsConfiguredValue() {
        assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(ACCESS_EXP);
    }

    // ── helpers — build tokens directly with the same HMAC secret ─────────────

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String tokenWithoutAud(String subject) {
        return Jwts.builder().subject(subject)
                .claim(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_CUSTOMER)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key(), Jwts.SIG.HS256).compact();
    }

    private String tokenWithAud(String subject, String aud) {
        return Jwts.builder().subject(subject)
                .claim(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_CUSTOMER)
                .audience().add(aud).and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key(), Jwts.SIG.HS256).compact();
    }

    private String tokenWithoutTokenTypeClaim(String subject) {
        return Jwts.builder().subject(subject)
                .audience().add(JwtConstants.AUDIENCE_CUSTOMER).and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key(), Jwts.SIG.HS256).compact();
    }

    private String tokenWithSubject(String subject) {
        return Jwts.builder().subject(subject)
                .claim(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_CUSTOMER)
                .audience().add(JwtConstants.AUDIENCE_CUSTOMER).and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key(), Jwts.SIG.HS256).compact();
    }

    private String expiredToken(String subject) {
        return Jwts.builder().subject(subject)
                .claim(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_CUSTOMER)
                .audience().add(JwtConstants.AUDIENCE_CUSTOMER).and()
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key(), Jwts.SIG.HS256).compact();
    }
}
