package com.vsk.devtrust.registry;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Named MonitoredService rather than plain "Service" on purpose — the
 * existing codebase already has ServiceGraphService, ServiceNode, and a
 * whole com.vsk.devtrust.service package. A bare "Service" here would be
 * confusing to read next to those, even though Java doesn't actually collide
 * on the class name across packages.
 *
 * This is a config store, not yet a live integration point: today,
 * PrometheusAnomalyDetector and GitHubWebhookController still read their
 * thresholds and secrets from static config, not from rows in this table.
 * Wiring those two to read from here instead is the natural follow-up, and
 * it WILL require editing those two existing files — deliberately not done
 * in this pass, see README.md.
 */
@Entity
@Table(name = "monitored_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoredService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serviceName;

    private String repoUrl;

    // Per-service webhook secret override — not read by GitHubWebhookController
    // yet, which still uses the single global GITHUB_WEBHOOK_SECRET. Stored
    // here in preparation for that wiring.
    private String webhookSecret;

    // Informational for now — see the "Prometheus limitation" note in README.md
    // about why adding a row here doesn't automatically make Prometheus scrape it.
    private String prometheusTarget;

    @Builder.Default
    private Double latencyThresholdMs = 200.0;

    @Builder.Default
    private Double heapThresholdPercent = 75.0;

    @Builder.Default
    private String businessTier = "TIER_2";

    @Builder.Default
    private boolean active = true;

    private Instant createdAt;
    private Instant updatedAt;
}
