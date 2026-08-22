package com.vsk.devtrust.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless JWT API — no server-side session, no CSRF token needed
                // (CSRF matters for cookie-based auth; a Bearer header isn't
                // automatically attached by the browser, so it isn't exploitable
                // the same way).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Login/register must be reachable without already being logged in
                        .requestMatchers("/api/auth/**").permitAll()
                        // GitHub calls this directly — it authenticates itself via
                        // HMAC signature (GitHubWebhookController), not a JWT
                        .requestMatchers("/webhooks/**").permitAll()
                        // Prometheus scrapes this on a schedule with no login of its own
                        .requestMatchers("/actuator/prometheus", "/actuator/health").permitAll()
                        // The WebSocket handshake itself is allowed through here —
                        // the real check happens in WebSocketAuthInterceptor via the
                        // token query param, since SockJS's fallback negotiation
                        // touches several sub-paths under /ws/** that shouldn't each
                        // need to be enumerated here.
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
