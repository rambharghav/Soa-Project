package com.payvault.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    public static final String DEFAULT_TEST_SECRET =
            "TestOnlySecretKeyForAutomatedUnitAndIntegrationTests2026!";

    private final SecretKey key;

    public JwtUtil(
            @Value("${jwt.secret}") String secretKey
    ) {
        if (secretKey == null || secretKey.trim().length() < 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET environment variable is required and must be at least 32 characters (256 bits)"
            );
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Test-only convenience constructor for unit tests that do not load Spring context.
     */
    public JwtUtil() {
        this(DEFAULT_TEST_SECRET);
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());

        } catch (Exception e) {
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token)
                .get("role", String.class);
    }
}