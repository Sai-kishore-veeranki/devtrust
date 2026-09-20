package com.vsk.devtrust.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final InviteService inviteService;

    public record LoginRequest(String username, String password) {}
    public record RegisterRequest(String username, String email, String fullName, String password) {}
    public record RegisterWithInviteRequest(String token, String username, String fullName, String password) {}

    @GetMapping("/setup-status")
    public ResponseEntity<Map<String, Boolean>> setupStatus() {
        return ResponseEntity.ok(Map.of("needsSetup", authService.needsSetup()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        try {
            String token = authService.register(req.username(), req.email(), req.fullName(), req.password());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            String token = authService.login(req.username(), req.password());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    // --- Invite flow — both public, the invitee isn't logged in yet ---

    @GetMapping("/invites/{token}")
    public ResponseEntity<Map<String, Object>> checkInvite(@PathVariable String token) {
        InviteService.InviteStatus status = inviteService.checkInvite(token);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("valid", status.valid());
        body.put("email", status.email());
        body.put("reason", status.reason());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/register-with-invite")
    public ResponseEntity<?> registerWithInvite(@RequestBody RegisterWithInviteRequest req) {
        try {
            String token = inviteService.registerWithInvite(req.token(), req.username(), req.fullName(), req.password());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
