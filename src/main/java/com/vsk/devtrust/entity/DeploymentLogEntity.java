package com.vsk.devtrust.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Records every deployment DevTrust observes via the GitHub webhook,
 * regardless of whether it ever correlates with an anomaly.
 *
 * IncidentEntity only exists for deployments that caused a problem, so it
 * can never be used as the denominator for "how many deployments happened
 * in total" — that was the root cause of change-failure-rate always
 * reporting 100%. This table is the real deployment log DORA metrics need.
 */
@Entity
@Table(name = "deployment_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deploymentId;
    private String commitId;
    private String author;
    private String serviceName;
    private String environment;
    private Instant deployedAt;
}
