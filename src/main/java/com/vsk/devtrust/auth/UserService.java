package com.vsk.devtrust.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(String username) {
        User user = findByUsername(username);
        return UserProfileResponse.from(user);
    }

    /**
     * Username is deliberately not editable here — it's the JWT subject,
     * and letting it change would mean every token issued before the
     * change stops matching the account it was issued for. Only email
     * and fullName are updatable; changing your login name is a bigger
     * operation this endpoint isn't trying to be.
     */
    public UserProfileResponse updateProfile(String username, String newEmail, String newFullName) {
        User user = findByUsername(username);

        if (newEmail != null && !newEmail.isBlank()) {
            String normalized = newEmail.trim().toLowerCase();
            if (!EMAIL_PATTERN.matcher(normalized).matches()) {
                throw new IllegalArgumentException("A valid email address is required.");
            }
            user.setEmail(normalized);
        }
        if (newFullName != null && !newFullName.isBlank()) {
            user.setFullName(newFullName.trim());
        }

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("That email is already in use by another account.");
        }

        return UserProfileResponse.from(user);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = findByUsername(username);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<UserProfileResponse> listAllUsers() {
        return userRepository.findAll().stream()
                .map(UserProfileResponse::from)
                .toList();
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found."));
    }
}
