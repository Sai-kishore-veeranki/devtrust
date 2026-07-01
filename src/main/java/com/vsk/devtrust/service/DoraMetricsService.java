package com.vsk.devtrust.service;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoraMetricsService {

    private final IncidentRepository incidentRepository;

    public Map<String, Object> computeMetrics(int lastDays) {
        Instant since = Instant.now().minus(lastDays, ChronoUnit.DAYS);
        List<IncidentEntity> incidents = incidentRepository
                .findByDetectedAtAfterOrderByDetectedAtDesc(since);

        return Map.of(
                "period_days", lastDays,
                "deployment_frequency", computeDeploymentFrequency(incidents, lastDays),
                "change_failure_rate", computeChangeFailureRate(incidents),
                "mean_time_to_recovery_minutes", computeMttr(incidents),
                "total_incidents", incidents.size(),
                "open_incidents", incidents.stream().filter(i -> "OPEN".equals(i.getStatus())).count(),
                "resolved_incidents", incidents.stream().filter(i -> "RESOLVED".equals(i.getStatus())).count()
        );
    }

    private double computeDeploymentFrequency(List<IncidentEntity> incidents, int lastDays) {
        long uniqueDeployments = incidents.stream()
                .map(IncidentEntity::getCommitId)
                .distinct()
                .count();
        return lastDays > 0 ? (double) uniqueDeployments / lastDays : 0;
    }

    private double computeChangeFailureRate(List<IncidentEntity> incidents) {
        long totalDeployments = incidents.stream()
                .map(IncidentEntity::getCommitId)
                .distinct()
                .count();

        if (totalDeployments == 0) return 0.0;

        long failedDeployments = incidents.stream()
                .map(IncidentEntity::getCommitId)
                .distinct()
                .count();

        return Math.round(((double) failedDeployments / totalDeployments * 100) * 100.0) / 100.0;
    }

    private double computeMttr(List<IncidentEntity> incidents) {
        List<IncidentEntity> resolved = incidents.stream()
                .filter(i -> "RESOLVED".equals(i.getStatus()) && i.getResolvedAt() != null)
                .collect(Collectors.toList());

        if (resolved.isEmpty()) return 0.0;

        double totalMinutes = resolved.stream()
                .mapToLong(i -> ChronoUnit.MINUTES.between(i.getDetectedAt(), i.getResolvedAt()))
                .average()
                .orElse(0.0);

        return Math.round(totalMinutes * 100.0) / 100.0;
    }
}