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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RootCauseAnalysisService {

    private final RestTemplate restTemplate = createRestTemplateWithTimeout();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${devtrust.ai.api-key}")
    private String apiKey;

    @Value("${devtrust.ai.api-url}")
    private String apiUrl;

    @Value("${devtrust.ai.model}")
    private String model;

    private static RestTemplate createRestTemplateWithTimeout() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

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
            JsonNode root = objectMapper.readTree(response);

            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            log.error("Failed to generate root cause analysis for incident [{}]", incident.getIncidentId(), e);
            return "AI analysis unavailable. Manual investigation required.";
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