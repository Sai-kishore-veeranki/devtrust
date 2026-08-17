package com.vsk.devtrust.service;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.repository.DeploymentLogRepository;
import com.vsk.devtrust.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoraMetricsServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private DeploymentLogRepository deploymentLogRepository;

    /**
     * Before the fix, computeChangeFailureRate derived both the numerator and the
     * denominator from the same incidents list, so the rate was always 100%
     * regardless of how many total deployments happened. This pins the real
     * behaviour: 2 incidents out of 10 total deployments should read 20%, not 100%.
     */
    @Test
    void changeFailureRate_reflectsRealDeploymentTotal_notJustIncidentCount() {
        DoraMetricsService service = new DoraMetricsService(incidentRepository, deploymentLogRepository);

        List<IncidentEntity> incidents = List.of(
                incidentWithCommit("commit-a"),
                incidentWithCommit("commit-b")
        );

        when(incidentRepository.findByDetectedAtAfterOrderByDetectedAtDesc(any(Instant.class)))
                .thenReturn(incidents);
        when(deploymentLogRepository.countByDeployedAtAfter(any(Instant.class)))
                .thenReturn(10L);

        Map<String, Object> metrics = service.computeMetrics(30);

        assertThat(metrics.get("change_failure_rate")).isEqualTo(20.0);
        assertThat(metrics.get("total_deployments")).isEqualTo(10L);
    }

    @Test
    void changeFailureRate_isZero_whenNoDeploymentsObserved() {
        DoraMetricsService service = new DoraMetricsService(incidentRepository, deploymentLogRepository);

        when(incidentRepository.findByDetectedAtAfterOrderByDetectedAtDesc(any(Instant.class)))
                .thenReturn(List.of());
        when(deploymentLogRepository.countByDeployedAtAfter(any(Instant.class)))
                .thenReturn(0L);

        Map<String, Object> metrics = service.computeMetrics(30);

        assertThat(metrics.get("change_failure_rate")).isEqualTo(0.0);
    }

    private IncidentEntity incidentWithCommit(String commitId) {
        IncidentEntity incident = new IncidentEntity();
        incident.setCommitId(commitId);
        incident.setStatus("OPEN");
        return incident;
    }
}
