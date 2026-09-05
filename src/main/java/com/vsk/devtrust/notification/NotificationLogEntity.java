package com.vsk.devtrust.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Deliberately a separate table rather than a "notified" column added to
 * IncidentEntity — this module ships as a pure addition with zero edits to
 * any existing file, and this is the piece that makes that possible while
 * still guaranteeing each incident triggers at most one Slack message.
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
