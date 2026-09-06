package com.vsk.devtrust.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Separate table rather than a "notified" column added to IncidentEntity —
 * keeps this module a pure addition with zero edits to any existing file,
 * while still guaranteeing each incident triggers at most one email.
 */
@Entity
@Table(name = "notification_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String incidentId;

    private String channel;
    private Instant notifiedAt;
}
