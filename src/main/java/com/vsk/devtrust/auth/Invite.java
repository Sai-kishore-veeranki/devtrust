package com.vsk.devtrust.auth;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * An invite is tied to a specific email an ADMIN chose — not an open,
 * shareable signup link. That's a deliberate choice consistent with why
 * registration was bootstrap-only in the first place: a dashboard showing
 * revenue-at-risk figures shouldn't have a link that works for anyone who
 * gets hold of it, only for the person it was actually issued to.
 */
@Entity
@Table(name = "invite")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private String invitedEmail;

    @Column(nullable = false)
    private String createdByUsername;

    private Instant createdAt;
    private Instant expiresAt;

    @Builder.Default
    private boolean used = false;

    private Instant usedAt;
}
