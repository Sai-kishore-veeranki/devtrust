package com.vsk.devtrust.notification;

import com.vsk.devtrust.entity.IncidentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * Reuses the shared RestTemplate bean from HttpClientConfig (already has
 * sane connect/read timeouts configured) rather than creating a new one —
 * one more reason this module needs no edits to existing files.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackNotifier {

    private final RestTemplate restTemplate;

    // No fallback secret pattern here on purpose, unlike GROQ_API_KEY or
    // GITHUB_WEBHOOK_SECRET — notifications are opt-in. An empty value means
    // "not configured," not "misconfigured," so this must never fail startup.
    @Value("${DEVTRUST_SLACK_WEBHOOK_URL:}")
    private String webhookUrl;

    @PostConstruct
    void logStatus() {
        if (isEnabled()) {
            log.info("Slack notifications enabled.");
        } else {
            log.info("Slack notifications disabled — set DEVTRUST_SLACK_WEBHOOK_URL to enable.");
        }
    }

    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    public void send(IncidentEntity incident) {
        if (!isEnabled()) {
            return;
        }

        String emoji = switch (incident.getSeverity()) {
            case "CRITICAL" -> ":rotating_light:";
            case "HIGH" -> ":warning:";
            default -> ":large_yellow_circle:";
        };

        String text = String.format(
                "%s *%s incident — %s*\n" +
                "*Metric:* `%s` = %.1f (threshold %.1f)\n" +
                "*Deploy:* `%s` by %s — %ds before the anomaly (%.0f%% confidence)\n" +
                "*Impact:* $%.2f at risk, ~%d users affected%s",
                emoji,
                incident.getSeverity(),
                incident.getServiceName(),
                incident.getMetricName(),
                incident.getAnomalyValue(),
                incident.getThreshold(),
                incident.getCommitId(),
                incident.getAuthor(),
                incident.getDeltaSeconds(),
                incident.getConfidenceScore() * 100,
                incident.getEstimatedRevenueLost(),
                incident.getEstimatedUsersAffected(),
                incident.isSlaBreached() ? "\n:x: *SLA BREACHED*" : ""
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("text", text), headers);
            restTemplate.postForObject(webhookUrl, request, String.class);
        } catch (Exception e) {
            // A failed Slack call should never take down the polling job or
            // affect incident detection itself — log and move on.
            log.error("Failed to send Slack notification for incident [{}]", incident.getIncidentId(), e);
        }
    }
}
