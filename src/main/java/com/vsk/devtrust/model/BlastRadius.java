package com.vsk.devtrust.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BlastRadius {
    private String serviceName;
    private String tier;
    private double estimatedRevenueLost;
    private double estimatedUsersAffected;
    private long durationMinutes;
    private boolean slaBreached;
    private int slaThresholdMinutes;
    private String costSummary;
}