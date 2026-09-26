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
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackNotifier implements NotificationProvider {

    private final NotificationProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SLACK;
    }

    @Override
    public boolean isEnabled() {
        String webhookUrl = properties.getSlack().getWebhookUrl();
        return properties.getSlack().isEnabled() && webhookUrl != null && !webhookUrl.isBlank();
    }

    @Override
    public void sendAlert(IncidentEntity incident) throws Exception {
        Map<String, Object> payload = Map.of(
                "attachments", List.of(Map.of(
                        "color", severityColor(incident.getSeverity()),
                        "blocks", List.of(
                                Map.of("type", "header", "text", Map.of(
                                        "type", "plain_text",
                                        "text", "DevTrust Alert: " + incident.getMetricName(),
                                        "emoji", true)),
                                Map.of("type", "section", "fields", List.of(
                                        field("Service", incident.getServiceName()),
                                        field("Severity", incident.getSeverity()),
                                        field("Status", incident.getStatus()),
                                        field("Incident ID", incident.getIncidentId()))),
                                Map.of("type", "section", "text", Map.of(
                                        "type", "mrkdwn",
                                        "text", "*Details:*\n" + safe(incident.getRootCauseAnalysis(),
                                                "Anomaly detected for " + incident.getMetricName())))
                        )
                ))
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getSlack().getWebhookUrl()))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Slack API responded with status "
                    + response.statusCode() + ": " + response.body());
        }
        log.info("Slack alert dispatched for incident {}", incident.getIncidentId());
    }

    private Map<String, String> field(String name, Object value) {
        return Map.of("type", "mrkdwn", "text", "*" + name + ":*\n" + safe(value, "N/A"));
    }

    private String severityColor(String severity) {
        if (severity == null) return "#808080";
        return switch (severity.toUpperCase()) {
            case "CRITICAL", "SEV1" -> "#E01E5A";
            case "HIGH", "SEV2" -> "#ECB22E";
            case "MEDIUM", "SEV3" -> "#2EB886";
            default -> "#439FE0";
        };
    }

    private String safe(Object value, String fallback) {
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }
}
