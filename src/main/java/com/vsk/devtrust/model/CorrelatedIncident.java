package com.vsk.devtrust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrelatedIncident {
    private String incidentId;
    private DeploymentEvent deployment;
    private AnomalyEvent anomaly;
    private long deltaSeconds;        // time between deploy and anomaly
    private double confidenceScore;   // 0.0 to 1.0
    private Instant detectedAt;
}