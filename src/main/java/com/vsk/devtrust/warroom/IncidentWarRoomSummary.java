package com.vsk.devtrust.warroom;

import java.util.List;

public record IncidentWarRoomSummary(
        String primaryService,
        String primarySeverity,
        String headline,
        double totalRevenueLost,
        int impactedServiceCount,
        List<String> impactedServices,
        List<String> recommendedActions
) {
}
