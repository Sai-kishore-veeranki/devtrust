package com.vsk.devtrust.warroom;

public record IncidentSummaryInput(
        String incidentId,
        String serviceName,
        String severity,
        double estimatedRevenueLost,
        long affectedUsers,
        double p95LatencyMs,
        String rootCauseHint
) {
}
