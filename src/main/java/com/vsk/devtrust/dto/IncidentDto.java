package com.vsk.devtrust.dto;

import java.time.Instant;

public class IncidentDto {
    public String incidentId;
    public String serviceName;
    public String commitId;
    public String author;
    public String metricName;
    public double anomalyValue;
    public double threshold;
    public String severity;
    public long deltaSeconds;
    public double confidenceScore;
    public Instant detectedAt;
    public String status;
    public Instant resolvedAt;
    public String resolvedBy;
    public Double estimatedRevenueLost;
    public Double estimatedUsersAffected;
    public Double durationMinutes;
    public boolean slaBreached;
    public String costSummary;
    public String rootCauseAnalysis;
}
