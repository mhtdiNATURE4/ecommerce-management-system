package com.market.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // default to 1 hour (ms) if not provided
    @Value("${jwt.expiration:3600000}")
    private long expiration;

    @Value("${jwt.secret:}")
    private String secret;

    private final Environment env;

    public JwtUtil(Environment env) {
        this.env = env;
    }

    private SecretKey getKey() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret (jwt.secret) is not configured. Set environment variable JWT_SECRET or application property jwt.secret");
        }
        if (secret.startsWith("CHANGE_THIS") || secret.length() < 32) {
            throw new IllegalStateException("JWT secret appears insecure; set a sufficiently long random jwt.secret (32+ chars)");
        }

        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @PostConstruct
    private void validateSecretOnStartup() {
        boolean isTest = env != null && env.acceptsProfiles(Profiles.of("test", "dev"));
        if (isTest) {
            return;
        }

        // perform validation and fail-fast on insecure/missing secret
        getKey();
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getKey())
            .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        try {
            // Use explicit JJWT 0.12.5 parsing API per project requirement.
            // This uses the SecretKey produced by getKey() and parses signed claims.
            return (Claims) Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // rethrow JwtException so callers (validateToken) can treat as invalid
            throw e;
        } catch (Exception e) {
            throw new JwtException("Failed to parse JWT token", e);
        }
    }
}
