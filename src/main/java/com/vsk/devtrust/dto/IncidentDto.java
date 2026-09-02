package com.vsk.devtrust.dto;

import java.time.Instant;

public class IncidentDto {
    public String incidentId;
    public String serviceName;
    public String commitId;
    public Instant detectedAt;
    public String status;
    public Instant resolvedAt;
    public Double estimatedRevenueLost;
    public Double estimatedUsersAffected;
    public Double durationMinutes;
    public String costSummary;
}
