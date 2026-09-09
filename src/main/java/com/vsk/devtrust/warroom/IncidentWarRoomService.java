package com.vsk.devtrust.warroom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IncidentWarRoomService {

    public IncidentWarRoomSummary buildSummary(List<IncidentSummaryInput> incidents) {
        if (incidents == null || incidents.isEmpty()) {
            return new IncidentWarRoomSummary(
                    "No active incident",
                    "INFO",
                    "No operational incidents are active in the monitored surface area.",
                    0.0,
                    0,
                    List.of(),
                    List.of(
                            "Keep monitoring baseline metrics and verify no silent degradation is resurfacing.",
                            "Review recent deployment windows for any unverified change set."
                    )
            );
        }

        List<IncidentSummaryInput> ordered = incidents.stream()
                .sorted(Comparator.comparingDouble(this::severityWeight).reversed())
                .toList();

        IncidentSummaryInput primary = ordered.getFirst();

        Map<String, Long> serviceCounts = incidents.stream()
                .collect(Collectors.groupingBy(IncidentSummaryInput::serviceName, LinkedHashMap::new, Collectors.counting()));

        List<String> impactedServices = serviceCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();

        double totalRevenueLost = incidents.stream()
                .mapToDouble(IncidentSummaryInput::estimatedRevenueLost)
                .sum();

        String headline = "Primary incident on " + primary.serviceName() + " is " + primary.severity() +
                " with a possible impact of " + formatCurrency(totalRevenueLost) + ".";

        List<String> actions = buildRecommendedActions(primary, impactedServices, totalRevenueLost);

        return new IncidentWarRoomSummary(
                primary.serviceName(),
                primary.severity(),
                headline,
                totalRevenueLost,
                impactedServices.size(),
                impactedServices,
                actions
        );
    }

    private List<String> buildRecommendedActions(
            IncidentSummaryInput primary,
            List<String> impactedServices,
            double totalRevenueLost
    ) {
        List<String> actions = new ArrayList<>();

        actions.add("Open the incident war room for " + primary.serviceName() + " and assign an incident commander immediately.");
        actions.add("Pull the latest deployment and code-change context for the service that matches the top severity signal.");
        actions.add("Check the hottest dependency chain first: " + String.join(", ", impactedServices));
        actions.add("Escalate if p95 latency exceeds 2x baseline or if revenue loss is above " + formatCurrency(totalRevenueLost * 0.2) + ".");

        if (primary.rootCauseHint() != null && !primary.rootCauseHint().isBlank()) {
            actions.add("Validate the root-cause lead: " + primary.rootCauseHint());
        }

        actions.add("Capture customer impact summary, rollback decision, and recovery checklist before closing the war room.");
        return actions;
    }

    private double severityWeight(IncidentSummaryInput incident) {
        return switch (incident.severity().toUpperCase()) {
            case "CRITICAL" -> 5.0;
            case "HIGH" -> 4.0;
            case "MEDIUM" -> 2.5;
            case "LOW" -> 1.0;
            default -> 0.5;
        } + (incident.p95LatencyMs() / 1000.0);
    }

    private String formatCurrency(double amount) {
        return String.format("$%.2f", amount);
    }
}
