package com.vsk.devtrust.runbook;

import java.time.Instant;
import java.util.List;

public record RunbookExecutionResult(
        Long runbookId,
        String title,
        String status,
        String summary,
        List<String> executedSteps,
        Instant startedAt,
        Instant completedAt
) {
}
