package com.vsk.devtrust.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${devtrust.jwt.secret}")
    private String secret;

    @Value("${devtrust.jwt.expiration-minutes}")
    private long expirationMinutes;

    @PostConstruct
    private void validateKey() {
        try {
            key();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Configured JWT secret is too weak; it must be at least 32 bytes.", e);
        }
    }

    private SecretKey key() {
        // HMAC-SHA256 needs a key of at least 256 bits (32 bytes). Fails
        // fast and clearly at startup if a weak DEVTRUST_JWT_SECRET was
        // configured, instead of a confusing crypto exception at request time.
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMinutes * 60_000);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key())
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
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
        // Use the parserBuilder API to set the signing key and parse a JWS
        // which returns a Jws<Claims> whose body is the Claims instance.
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
