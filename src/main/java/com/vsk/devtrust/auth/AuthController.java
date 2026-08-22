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

    public record Credentials(String username, String password) {}

    @GetMapping("/setup-status")
    public ResponseEntity<Map<String, Boolean>> setupStatus() {
        return ResponseEntity.ok(Map.of("needsSetup", authService.needsSetup()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Credentials req) {
        try {
            String token = authService.register(req.username(), req.password());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credentials req) {
        try {
            String token = authService.login(req.username(), req.password());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }
}
