package com.vsk.devtrust.runbook;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record RunbookResponse(
        Long id,
        String slug,
        String title,
        String serviceName,
        RunbookSeverity severity,
        String trigger,
        String summary,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<RunbookStep> steps,
        Set<String> owners
) {
    public static RunbookResponse fromEntity(RunbookEntity entity) {
        return new RunbookResponse(
                entity.getId(),
                entity.getSlug(),
                entity.getTitle(),
                entity.getServiceName(),
                entity.getSeverity(),
                entity.getTrigger(),
                entity.getSummary(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                List.copyOf(entity.getSteps()),
                Set.copyOf(entity.getOwners())
        );
    }
}
