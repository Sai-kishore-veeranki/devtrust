package com.vsk.devtrust.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsk.devtrust.entity.IncidentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RootCauseAnalysisService {

    private static final String FALLBACK_MESSAGE = "AI analysis unavailable. Manual investigation required.";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${devtrust.ai.api-key}")
    private String apiKey;

    @Value("${devtrust.ai.api-url}")
    private String apiUrl;

    @Value("${devtrust.ai.model}")
    private String model;

    public String generateRootCause(IncidentEntity incident) {
        try {
            String prompt = buildPrompt(incident);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "max_completion_tokens", 300,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(apiUrl, request, String.class);
            return extractContent(response, incident.getIncidentId());

        } catch (Exception e) {
            log.error("Failed to generate root cause analysis for incident [{}]", incident.getIncidentId(), e);
            return FALLBACK_MESSAGE;
        }
    }

    /**
     * Pulls choices[0].message.content out of the response — but verifies the
     * shape at every step instead of trusting it. JsonNode#asText() will happily
     * stringify a number, a null, or anything else it lands on with no error,
     * so a wrong path (or an unexpected response shape from the provider) fails
     * *silently* as a plausible-looking string instead of an obvious error.
     * That's how a stray usage.queue_time-style float can end up rendered as if
     * it were the analysis. Logging the raw body here means the next time the
     * shape is wrong, the logs show exactly what came back instead of us
     * guessing from the symptom.
     */
    private String extractContent(String rawResponse, String incidentId) {
        JsonNode root = readTreeSafely(rawResponse, incidentId);
        if (root == null) return FALLBACK_MESSAGE;

        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            log.error("Groq response for incident [{}] had no choices[]. Raw body: {}", incidentId, rawResponse);
            return FALLBACK_MESSAGE;
        }

        JsonNode message = choices.get(0).path("message");
        JsonNode content = message.path("content");

        if (!content.isTextual() || content.asText().isBlank()) {
            String finishReason = choices.get(0).path("finish_reason").asText("unknown");
            log.error("Groq response for incident [{}] had non-text or empty content (finish_reason={}). Raw body: {}",
                    incidentId, finishReason, rawResponse);
            return FALLBACK_MESSAGE;
        }

        return content.asText();
    }

    private JsonNode readTreeSafely(String rawResponse, String incidentId) {
        try {
            return objectMapper.readTree(rawResponse);
        } catch (Exception e) {
            log.error("Could not parse Groq response as JSON for incident [{}]. Raw body: {}",
                    incidentId, rawResponse, e);
            return null;
        }
    }

    private String buildPrompt(IncidentEntity incident) {
        return """
                You are a senior site reliability engineer reviewing a production incident.
                Write a concise root cause analysis in 3-4 sentences, plain text, no markdown.

                Incident details:
                - Service: %s
                - Deployed commit: %s by %s
                - Metric affected: %s
                - Measured value: %.2f (threshold: %.2f)
                - Severity: %s
                - Time between deployment and anomaly: %d seconds
                - Confidence this deployment caused the anomaly: %.0f%%

                Explain the likely root cause in plain engineering language, and suggest one concrete next step.
                """.formatted(
                incident.getServiceName(),
                incident.getCommitId(),
                incident.getAuthor(),
                incident.getMetricName(),
                incident.getAnomalyValue(),
                incident.getThreshold(),
                incident.getSeverity(),
                incident.getDeltaSeconds(),
                incident.getConfidenceScore() * 100
        );
    }
}