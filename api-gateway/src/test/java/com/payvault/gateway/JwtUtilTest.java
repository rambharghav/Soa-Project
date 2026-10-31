package com.payvault.gateway;

import com.payvault.gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String DEFAULT_SECRET = JwtUtil.DEFAULT_TEST_SECRET;

    private String generateTestToken(String secret, String username, String role, long expirationOffsetMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationOffsetMs))
                .signWith(key)
                .compact();
    }

    @Test
    void jwtUtilShouldValidateProperTokenAndExtractClaims() {
        JwtUtil jwtUtil = new JwtUtil();
        String token = generateTestToken(DEFAULT_SECRET, "testuser", "USER", 60000L);

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("testuser", jwtUtil.extractUsername(token));
        assertEquals("USER", jwtUtil.extractRole(token));
    }

    @Test
    void jwtUtilShouldSupportCustomSecretKey() {
        String customSecret = "CustomSecretKeyForGatewayUnitTestingPurpose2026";
        JwtUtil customJwtUtil = new JwtUtil(customSecret);

        String customToken = generateTestToken(customSecret, "adminuser", "ADMIN", 60000L);
        assertTrue(customJwtUtil.isTokenValid(customToken));
        assertEquals("adminuser", customJwtUtil.extractUsername(customToken));
        assertEquals("ADMIN", customJwtUtil.extractRole(customToken));

        // Token signed with default key should fail on custom secret
        String defaultToken = generateTestToken(DEFAULT_SECRET, "adminuser", "ADMIN", 60000L);
        assertFalse(customJwtUtil.isTokenValid(defaultToken));
    }

    @Test
    void jwtUtilShouldRejectExpiredToken() {
        JwtUtil jwtUtil = new JwtUtil();
        String expiredToken = generateTestToken(DEFAULT_SECRET, "testuser", "USER", -5000L);

        assertFalse(jwtUtil.isTokenValid(expiredToken));
    }

    @Test
    void jwtUtilShouldRejectTamperedToken() {
        JwtUtil jwtUtil = new JwtUtil();
        String token = generateTestToken(DEFAULT_SECRET, "testuser", "USER", 60000L);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(jwtUtil.isTokenValid(tamperedToken));
    }

    @Test
    void jwtUtilShouldRejectNullAndEmptyToken() {
        JwtUtil jwtUtil = new JwtUtil();

        assertFalse(jwtUtil.isTokenValid(null));
        assertFalse(jwtUtil.isTokenValid(""));
        assertFalse(jwtUtil.isTokenValid("   "));
    }

    @Test
    void jwtUtilShouldRejectShortOrMissingSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(null));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil(""));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("   "));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("short-secret-under-32-chars"));
    }
}
