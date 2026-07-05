package com.vsk.devtrust.service;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.entity.ServiceBusinessConfig;
import com.vsk.devtrust.model.BlastRadius;
import com.vsk.devtrust.repository.ServiceBusinessConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlastRadiusService {

    private final ServiceBusinessConfigRepository configRepository;

    // Default config for services without explicit config
    private static final ServiceBusinessConfig DEFAULT_CONFIG = ServiceBusinessConfig.builder()
            .revenuePerMinute(100.0)
            .activeUsersPerMinute(200.0)
            .slaThresholdMinutes(30)
            .tier("TIER_2")
            .build();

    public BlastRadius compute(IncidentEntity incident, Instant resolvedAt) {
        ServiceBusinessConfig config = configRepository
                .findByServiceName(incident.getServiceName())
                .orElse(DEFAULT_CONFIG);

        Instant end = resolvedAt != null ? resolvedAt : Instant.now();
        long durationMinutes = Math.max(1,
                ChronoUnit.MINUTES.between(incident.getDetectedAt(), end));

        double severityMultiplier = switch (incident.getSeverity()) {
            case "CRITICAL" -> 1.0;
            case "HIGH"     -> 0.75;
            case "MEDIUM"   -> 0.5;
            default         -> 0.25;
        };

        double revenueLost = config.getRevenuePerMinute()
                * durationMinutes
                * severityMultiplier;

        double usersAffected = config.getActiveUsersPerMinute()
                * durationMinutes
                * severityMultiplier;

        boolean slaBreached = durationMinutes > config.getSlaThresholdMinutes();

        String costSummary = buildCostSummary(
                revenueLost, usersAffected, durationMinutes, slaBreached, config);

        log.info("Blast radius computed for incident [{}]: revenue=${} users={} duration={}min sla_breached={}",
                incident.getIncidentId(),
                String.format("%.2f", revenueLost),
                String.format("%.0f", usersAffected),
                durationMinutes,
                slaBreached);

        return BlastRadius.builder()
                .serviceName(incident.getServiceName())
                .tier(config.getTier())
                .estimatedRevenueLost(Math.round(revenueLost * 100.0) / 100.0)
                .estimatedUsersAffected(Math.round(usersAffected))
                .durationMinutes(durationMinutes)
                .slaBreached(slaBreached)
                .slaThresholdMinutes(config.getSlaThresholdMinutes())
                .costSummary(costSummary)
                .build();
    }

    private String buildCostSummary(double revenueLost, double usersAffected,
                                    long durationMinutes, boolean slaBreached,
                                    ServiceBusinessConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("$%.0f estimated revenue impact over %d minute(s). ", revenueLost, durationMinutes));
        sb.append(String.format("~%.0f users potentially affected. ", usersAffected));

        if (slaBreached) {
            sb.append(String.format("SLA breached (exceeded %d min threshold). ", config.getSlaThresholdMinutes()));
        } else {
            sb.append(String.format("SLA within threshold (%d min limit). ", config.getSlaThresholdMinutes()));
        }

        sb.append(String.format("Service tier: %s.", config.getTier()));
        return sb.toString();
    }
}