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
public class AnomalyEvent {
    private String anomalyId;
    private String serviceName;
    private String metricName;        // "p99_latency", "error_rate", "cpu_usage"
    private double value;
    private double threshold;
    private String severity;          // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    private Instant timestamp;
}