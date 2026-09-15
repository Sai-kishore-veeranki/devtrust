package com.vsk.devtrust.dto;

import com.vsk.devtrust.entity.IncidentEntity;

public class IncidentMapper {
    public static IncidentDto toDto(IncidentEntity e) {
        if (e == null) return null;
        IncidentDto d = new IncidentDto();
        d.incidentId = e.getIncidentId();
        d.serviceName = e.getServiceName();
        d.commitId = e.getCommitId();
        d.author = e.getAuthor();
        d.metricName = e.getMetricName();
        d.anomalyValue = e.getAnomalyValue();
        d.threshold = e.getThreshold();
        d.severity = e.getSeverity();
        d.deltaSeconds = e.getDeltaSeconds();
        d.confidenceScore = e.getConfidenceScore();
        d.detectedAt = e.getDetectedAt();
        d.status = e.getStatus();
        d.resolvedAt = e.getResolvedAt();
        d.resolvedBy = e.getResolvedBy();
        d.estimatedRevenueLost = e.getEstimatedRevenueLost();
        d.estimatedUsersAffected = e.getEstimatedUsersAffected();
        d.durationMinutes = e.getDurationMinutes() != null ? Double.valueOf(e.getDurationMinutes()) : null;
        d.slaBreached = e.isSlaBreached();
        d.costSummary = e.getCostSummary();
        d.rootCauseAnalysis = e.getRootCauseAnalysis();
        return d;
    }
}
