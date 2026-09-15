package com.vsk.devtrust.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${devtrust.jwt.secret}")
    private String secret;

    @Value("${devtrust.jwt.expiration-minutes}")
    private long expirationMinutes;

    private SecretKey key;

    @PostConstruct
    void validateKey() {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "Configured JWT secret is too weak; it must be at least 32 bytes (256 bits) for HMAC-SHA256. " +
                    "Generate one with: openssl rand -base64 32");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Now takes the full User instead of just a username — the token needs
    // to carry email/fullName/role so the frontend can decode a real
    // identity from it, and so JwtAuthFilter can populate a real Spring
    // Security authority instead of an empty list.
    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMinutes * 60_000);

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
