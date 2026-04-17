package com.wd.custapi.service;

import com.wd.custapi.security.JwtConstants;
import com.wd.custapi.testsupport.TestcontainersPostgresBase;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class JwtAudienceTest extends TestcontainersPostgresBase {

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret}")
    private String secret;

    @Test
    void case1_newlyIssuedTokenCarriesAudClaim() {
        String token = jwtService.generateCustomerToken("testuser@t.com", new HashMap<>());
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Set<String> aud = claims.getAudience();
        assertThat(aud).contains(JwtConstants.AUDIENCE_CUSTOMER);
    }

    @Test
    void case2_correctAudEnforceFalseValidates(CapturedOutput output) {
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        String token = jwtService.generateCustomerToken("testuser@t.com", new HashMap<>());
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(output.getAll()).doesNotContain("JWT missing or mismatched aud claim");
    }

    @Test
    void case3_missingAudEnforceFalseValidatesAndLogsWarn(CapturedOutput output) {
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        String token = tokenWithoutAud();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(output.getAll()).contains("JWT missing or mismatched aud claim");
    }

    @Test
    void case4_wrongAudEnforceFalseValidatesAndLogsWarn(CapturedOutput output) {
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        String token = tokenWithAud(JwtConstants.AUDIENCE_PORTAL);
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(output.getAll()).contains("JWT missing or mismatched aud claim");
    }

    @Test
    void case5_correctAudEnforceTrueValidates() {
        ReflectionTestUtils.setField(jwtService, "audEnforce", true);
        try {
            String token = jwtService.generateCustomerToken("testuser@t.com", new HashMap<>());
            assertThat(jwtService.validateToken(token)).isTrue();
        } finally {
            ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        }
    }

    @Test
    void case6_missingAudEnforceTrueRejects() {
        ReflectionTestUtils.setField(jwtService, "audEnforce", true);
        try {
            String token = tokenWithoutAud();
            assertThat(jwtService.validateToken(token)).isFalse();
        } finally {
            ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        }
    }

    @Test
    void preChangeTokenStillWorksWhenEnforceFalse() {
        ReflectionTestUtils.setField(jwtService, "audEnforce", false);
        String legacyToken = tokenWithoutAud();
        assertThat(jwtService.validateToken(legacyToken)).isTrue();
    }

    private String tokenWithoutAud() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_CUSTOMER);
        return Jwts.builder().claims(claims).subject("testuser@t.com")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256).compact();
    }

    private String tokenWithAud(String audValue) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        HashMap<String, Object> claims = new HashMap<>();
        claims.put(JwtConstants.CLAIM_TOKEN_TYPE, JwtConstants.TOKEN_TYPE_CUSTOMER);
        return Jwts.builder().claims(claims).subject("testuser@t.com")
                .audience().add(audValue).and()
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS256).compact();
    }
}
