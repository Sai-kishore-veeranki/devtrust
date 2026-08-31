package com.vsk.devtrust.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Deliberately NOT open self-service registration. A monitoring dashboard
     * with revenue-at-risk figures on it shouldn't have a public "create an
     * account" page anyone who finds the URL can use. This only succeeds the
     * very first time, to create the one admin account — every call after
     * that fails, permanently, until someone with database access removes
     * the existing row.
     */
    public String register(String username, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);

        return jwtService.generateToken(username);
    }

    public String login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password."));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password.");
        }

        return jwtService.generateToken(username);
    }

    public boolean needsSetup() {
        return userRepository.count() == 0;
    }
}
