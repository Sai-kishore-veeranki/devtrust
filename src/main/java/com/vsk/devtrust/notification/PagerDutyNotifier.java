package com.vsk.devtrust.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsk.devtrust.entity.IncidentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PagerDutyNotifier implements NotificationProvider {

    private final NotificationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.PAGERDUTY;
    }

    @Override
    public boolean isEnabled() {
        String routingKey = properties.getPagerduty().getRoutingKey();
        return properties.getPagerduty().isEnabled() && routingKey != null && !routingKey.isBlank();
    }

    @Override
    public void sendAlert(IncidentEntity incident) throws Exception {
        Map<String, Object> payload = Map.of(
                "routing_key", properties.getPagerduty().getRoutingKey(),
                "event_action", "trigger",
                "dedup_key", "devtrust-incident-" + incident.getIncidentId(),
                "payload", Map.of(
                        "summary", String.format("[%s] %s: %s", incident.getSeverity(),
                                incident.getServiceName(), incident.getMetricName()),
                        "source", incident.getServiceName(),
                        "severity", mapSeverity(incident.getSeverity()),
                        "timestamp", Instant.now().toString(),
                        "custom_details", Map.of(
                                "incidentId", incident.getIncidentId(),
                                "description", incident.getRootCauseAnalysis() != null
                                        ? incident.getRootCauseAnalysis() : "",
                                "status", String.valueOf(incident.getStatus()))));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getPagerduty().getEventsApiUrl()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("PagerDuty Events API returned status "
                    + response.statusCode() + ": " + response.body());
        }
        log.info("PagerDuty alert dispatched for incident {}", incident.getIncidentId());
    }

    private String mapSeverity(String severity) {
        if (severity == null) return "info";
        return switch (severity.toUpperCase()) {
            case "CRITICAL", "SEV1" -> "critical";
            case "HIGH", "SEV2" -> "error";
            case "MEDIUM", "SEV3" -> "warning";
            default -> "info";
        };
    }
}
