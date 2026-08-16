package com.vsk.devtrust.service;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.repository.DeploymentLogRepository;
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
    private final DeploymentLogRepository deploymentLogRepository;

    public Map<String, Object> computeMetrics(int lastDays) {
        Instant since = Instant.now().minus(lastDays, ChronoUnit.DAYS);
        List<IncidentEntity> incidents = incidentRepository
                .findByDetectedAtAfterOrderByDetectedAtDesc(since);
        long totalDeployments = deploymentLogRepository.countByDeployedAtAfter(since);

        return Map.of(
                "period_days", lastDays,
                "deployment_frequency", computeDeploymentFrequency(totalDeployments, lastDays),
                "change_failure_rate", computeChangeFailureRate(incidents, totalDeployments),
                "mean_time_to_recovery_minutes", computeMttr(incidents),
                "total_deployments", totalDeployments,
                "total_incidents", incidents.size(),
                "open_incidents", incidents.stream().filter(i -> "OPEN".equals(i.getStatus())).count(),
                "resolved_incidents", incidents.stream().filter(i -> "RESOLVED".equals(i.getStatus())).count()
        );
    }

    private double computeDeploymentFrequency(long totalDeployments, int lastDays) {
        return lastDays > 0 ? (double) totalDeployments / lastDays : 0;
    }

    /**
     * Change failure rate = (deployments that caused an incident) / (all deployments).
     *
     * Previously this method derived BOTH the numerator and the denominator from
     * the same incidents list, so they were always equal and the rate always
     * printed 100% no matter what happened. The denominator now comes from
     * DeploymentLogRepository, which tracks every deployment DevTrust observes —
     * not just the ones that went on to cause a correlated incident.
     */
    private double computeChangeFailureRate(List<IncidentEntity> incidents, long totalDeployments) {
        if (totalDeployments == 0) return 0.0;

        long failedDeployments = incidents.stream()
                .map(IncidentEntity::getCommitId)
                .distinct()
                .count();

        double rate = (double) failedDeployments / totalDeployments * 100;
        return Math.round(Math.min(rate, 100.0) * 100.0) / 100.0;
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