package com.vsk.devtrust.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InviteService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int EXPIRY_DAYS = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public record InviteStatus(boolean valid, String email, String reason) {}
    public record CreatedInvite(String token, String invitedEmail, Instant expiresAt) {}

    public CreatedInvite createInvite(String invitedEmail, String createdByUsername) {
        if (invitedEmail == null || !EMAIL_PATTERN.matcher(invitedEmail.trim()).matches()) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (userRepository.existsByEmail(invitedEmail.trim().toLowerCase())) {
            throw new IllegalStateException("Someone with that email already has an account.");
        }

        String token = generateToken();
        Instant now = Instant.now();

        Invite invite = Invite.builder()
                .token(token)
                .invitedEmail(invitedEmail.trim().toLowerCase())
                .createdByUsername(createdByUsername)
                .createdAt(now)
                .expiresAt(now.plus(EXPIRY_DAYS, ChronoUnit.DAYS))
                .build();

        inviteRepository.save(invite);
        return new CreatedInvite(token, invite.getInvitedEmail(), invite.getExpiresAt());
    }

    public InviteStatus checkInvite(String token) {
        var inviteOpt = inviteRepository.findByToken(token);
        if (inviteOpt.isEmpty()) {
            return new InviteStatus(false, null, "This invite link is invalid.");
        }

        Invite invite = inviteOpt.get();
        if (invite.isUsed()) {
            return new InviteStatus(false, invite.getInvitedEmail(), "This invite has already been used.");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            return new InviteStatus(false, invite.getInvitedEmail(), "This invite has expired — ask an admin for a new one.");
        }

        return new InviteStatus(true, invite.getInvitedEmail(), null);
    }

    /**
     * Unlike AuthService.register() (bootstrap-only, always creates an
     * ADMIN), this always creates a MEMBER — an invited teammate never
     * gets admin access implicitly, regardless of who invited them.
     */
    @Transactional
    public String registerWithInvite(String token, String username, String fullName, String rawPassword) {
        InviteStatus status = checkInvite(token);
        if (!status.valid()) {
            throw new IllegalStateException(status.reason());
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required.");
        }

        Invite invite = inviteRepository.findByToken(token).orElseThrow();

        User user = User.builder()
                .username(username)
                .email(invite.getInvitedEmail())
                .fullName(fullName.trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.MEMBER)
                .createdAt(Instant.now())
                .build();

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("That username is already taken.");
        }

        invite.setUsed(true);
        invite.setUsedAt(Instant.now());
        inviteRepository.save(invite);

        return jwtService.generateToken(user);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
