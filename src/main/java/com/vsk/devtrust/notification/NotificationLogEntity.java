package com.vsk.devtrust.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notification_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_incident_channel",
                columnNames = {"incident_id", "channel"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private String incidentId;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    @Column(length = 2000)
    private String errorMessage;
}
