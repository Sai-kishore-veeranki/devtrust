package com.vsk.devtrust.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Deliberately still the only registration path — bootstrap-only,
     * succeeds exactly once. Phase B adds invite-based registration for
     * additional teammates; this method isn't the one that will handle
     * that, so it isn't being reworked to anticipate it.
     */
    @Transactional
    public String register(String username, String email, String fullName, String rawPassword) {
        if (userRepository.count() > 0) {
            throw new IllegalStateException("Setup already completed — registration is disabled. " +
                    "Contact whoever administers this deployment for account access.");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        User user = User.builder()
                .username(username)
                .email(email.trim().toLowerCase())
                .fullName(fullName.trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.ADMIN)
                .createdAt(Instant.now())
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Race-condition safety net, same principle as the correlation
            // key uniqueness check elsewhere in this project — the count()
            // check above isn't atomic, so a unique constraint at the DB
            // level is what actually guarantees only one bootstrap account
            // ever gets created if two requests land at the same instant.
            throw new IllegalStateException("That username or email is already taken.");
        }

        return jwtService.generateToken(user);
    }

    public String login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password."));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password.");
        }

        return jwtService.generateToken(user);
    }

    public boolean needsSetup() {
        return userRepository.count() == 0;
    }
}
