package com.vsk.devtrust.detector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsk.devtrust.model.AnomalyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusAnomalyDetector {

    private final RestTemplate restTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${devtrust.kafka.topics.anomalies}")
    private String anomaliesTopic;

    @Value("${devtrust.prometheus.url}")
    private String prometheusUrl;

    @Value("${devtrust.prometheus.service-name}")
    private String serviceName;

    @Value("${devtrust.detector.cooldown-seconds:300}")
    private long cooldownSeconds;

    // Ranks severities so a worsening breach can still re-alert mid-cooldown
    private static final List<String> SEVERITY_ORDER = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    // Tracks the last time each metric fired, so a metric that stays above
    // threshold for minutes doesn't create a new AnomalyEvent (and therefore a
    // new incident row) on every 15-second scrape.
    private final Map<String, LastAlert> lastAlertByMetric = new ConcurrentHashMap<>();

    private record LastAlert(Instant firedAt, String severity) {}

    // Checks real HTTP request latency every 15 seconds
    @Scheduled(fixedDelay = 15_000, initialDelay = 5_000)
    public void checkHttpLatency() {
        String query = "sum(rate(http_server_requests_seconds_sum[2m])) / sum(rate(http_server_requests_seconds_count[2m]))";
        checkMetric(query, "avg_response_time_ms", 200.0, value -> value * 1000);
    }

    // Checks JVM heap memory usage every 15 seconds
    @Scheduled(fixedDelay = 15_000, initialDelay = 8_000)
    public void checkMemoryUsage() {
        String query = "sum(jvm_memory_used_bytes{area=\"heap\"}) / sum(jvm_memory_max_bytes{area=\"heap\", id!=\"\"}) * 100";
        checkMetric(query, "heap_usage_percent", 75.0, value -> value);
    }

    private void checkMetric(String promQuery, String metricName, double threshold, java.util.function.DoubleUnaryOperator transform) {
        try {
            String url = prometheusUrl + "/api/v1/query?query={query}";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, promQuery);
            JsonNode root = objectMapper.readTree(response.getBody());

            JsonNode results = root.path("data").path("result");
            if (!results.isArray() || results.isEmpty()) {
                log.debug("No data yet for metric [{}] — service may need more traffic", metricName);
                return;
            }

            // Prometheus returns [timestamp, value] pairs as strings
            String rawValue = results.get(0).path("value").get(1).asText();
            double value = transform.applyAsDouble(Double.parseDouble(rawValue));

            log.info("Metric check: {} = {} (threshold: {})", metricName, String.format("%.2f", value), threshold);

            if (value > threshold) {
                String severity = computeSeverity(value, threshold);
                if (shouldAlert(metricName, severity)) {
                    publishAnomaly(metricName, value, threshold, severity);
                } else {
                    log.debug("Metric [{}] still above threshold but within cooldown — suppressing duplicate anomaly", metricName);
                }
            }

        } catch (Exception e) {
            log.warn("Failed to query Prometheus for metric [{}]: {}", metricName, e.getMessage());
        }
    }

    /**
     * Fires at most once per cooldown window per metric, unless the breach has
     * gotten worse since the last alert — a MEDIUM that escalates to CRITICAL
     * mid-cooldown should still notify immediately.
     */
    private boolean shouldAlert(String metricName, String severity) {
        LastAlert previous = lastAlertByMetric.get(metricName);

        boolean cooldownElapsed = previous == null
                || previous.firedAt().plusSeconds(cooldownSeconds).isBefore(Instant.now());
        boolean severityEscalated = previous != null
                && SEVERITY_ORDER.indexOf(severity) > SEVERITY_ORDER.indexOf(previous.severity());

        if (!cooldownElapsed && !severityEscalated) {
            return false;
        }

        lastAlertByMetric.put(metricName, new LastAlert(Instant.now(), severity));
        return true;
    }

    private void publishAnomaly(String metricName, double value, double threshold, String severity) {
        AnomalyEvent event = AnomalyEvent.builder()
                .anomalyId(UUID.randomUUID().toString())
                .serviceName(serviceName)
                .metricName(metricName)
                .value(value)
                .threshold(threshold)
                .severity(severity)
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(anomaliesTopic, serviceName, event);
        log.warn("REAL ANOMALY DETECTED: {} = {} exceeds threshold {} (severity: {})",
                metricName, String.format("%.2f", value), threshold, severity);
    }

    private String computeSeverity(double value, double threshold) {
        double ratio = value / threshold;
        if (ratio >= 2.0) return "CRITICAL";
        if (ratio >= 1.5) return "HIGH";
        if (ratio >= 1.2) return "MEDIUM";
        return "LOW";
    }
}