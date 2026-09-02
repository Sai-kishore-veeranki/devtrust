package com.vsk.devtrust.dto;

import com.vsk.devtrust.entity.IncidentEntity;

public class IncidentMapper {
    public static IncidentDto toDto(IncidentEntity e) {
        if (e == null) return null;
        IncidentDto d = new IncidentDto();
        d.incidentId = e.getIncidentId();
        d.serviceName = e.getServiceName();
        d.commitId = e.getCommitId();
        d.detectedAt = e.getDetectedAt();
        d.status = e.getStatus();
        d.resolvedAt = e.getResolvedAt();
        d.estimatedRevenueLost = e.getEstimatedRevenueLost();
        d.estimatedUsersAffected = e.getEstimatedUsersAffected();
        d.durationMinutes = e.getDurationMinutes();
        d.costSummary = e.getCostSummary();
        return d;
    }
}
