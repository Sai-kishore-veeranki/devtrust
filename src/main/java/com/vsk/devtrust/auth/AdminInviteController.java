package com.vsk.devtrust.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Under /api/admin/** on purpose — /api/auth/** is entirely permitAll
 * (login/register/setup-status all need to work without a JWT), so an
 * admin-only endpoint can't live there without carving out an exception
 * to that broad rule. A separate prefix falls under the existing
 * .anyRequest().authenticated() catch-all by default, then gets the
 * ADMIN-only restriction added on top in SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/invites")
@RequiredArgsConstructor
public class AdminInviteController {

    private final InviteService inviteService;

    public record InviteRequest(String email) {}

    @PostMapping
    public ResponseEntity<?> createInvite(@RequestBody InviteRequest req, Authentication authentication) {
        try {
            InviteService.CreatedInvite invite = inviteService.createInvite(req.email(), authentication.getName());
            return ResponseEntity.ok(Map.of(
                    "token", invite.token(),
                    "invitedEmail", invite.invitedEmail(),
                    "expiresAt", invite.expiresAt().toString()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
