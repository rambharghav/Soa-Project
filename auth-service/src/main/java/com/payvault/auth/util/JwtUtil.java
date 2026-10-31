package com.payvault.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    public static final String DEFAULT_TEST_SECRET =
            "TestOnlySecretKeyForAutomatedUnitAndIntegrationTests2026!";

    private final SecretKey key;
    private final long expirationTime;

    public JwtUtil(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.expiration-ms:3600000}") long expirationTime
    ) {
        if (secretKey == null || secretKey.trim().length() < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET environment variable is required and must be at least 32 characters (256 bits)"
            );
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    /**
     * Test-only convenience constructor for unit tests that do not load Spring context.
     */
    public JwtUtil() {
        this(DEFAULT_TEST_SECRET, 3600000L);
    }

    public String generateToken(String username, String role) {

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis()
                                + expirationTime)
                )
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {

        return getClaims(token)
                .getSubject();
    }

    public String extractRole(String token) {

        return getClaims(token)
                .get("role", String.class);
    }

    public boolean isTokenValid(String token) {

        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = getClaims(token);
            return claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}