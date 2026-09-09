# Incident War Room Feature

This feature adds a lightweight incident command center that can be used without changing the existing application flow.

## What it provides

- Picks the highest-priority incident from a list of active signals
- Aggregates impacted services and total revenue impact
- Produces a short emergency-summary headline
- Suggests next actions for an incident commander

## Suggested usage

```java
IncidentWarRoomService service = new IncidentWarRoomService();

List<IncidentSummaryInput> incidents = List.of(
    new IncidentSummaryInput("inc-1", "payments-service", "CRITICAL", 12450.0, 6200L, 980.0, "database connection leak"),
    new IncidentSummaryInput("inc-2", "gateway-service", "HIGH", 4600.0, 3100L, 640.0, "increased retry bursts")
);

IncidentWarRoomSummary summary = service.buildSummary(incidents);
```

This can later be wired into a dashboard, webhook, or Slack/Teams notification without impacting the current code paths.
