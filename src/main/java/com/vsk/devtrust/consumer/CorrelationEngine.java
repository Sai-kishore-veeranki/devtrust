package com.vsk.devtrust.consumer;

import com.vsk.devtrust.model.AnomalyEvent;
import com.vsk.devtrust.model.CorrelatedIncident;
import com.vsk.devtrust.model.DeploymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CorrelationEngine {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // In-memory window: serviceName -> most recent DeploymentEvent
    // On Day 3 you'll replace this with Redis for persistence
    private final Map<String, DeploymentEvent> recentDeployments = new ConcurrentHashMap<>();

    @Value("${devtrust.kafka.topics.incidents}")
    private String incidentsTopic;

    @Value("${devtrust.correlation.window-seconds}")
    private long correlationWindowSeconds;

    public CorrelationEngine(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Listens to deployment events and stores them in the correlation window
    @KafkaListener(
            topics = "${devtrust.kafka.topics.deployments}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onDeployment(@Payload DeploymentEvent event) {
        log.info("Received deployment: service={} commit={} author={}",
                event.getServiceName(), event.getCommitId(), event.getAuthor());

        recentDeployments.put(event.getServiceName(), event);
    }

    // Listens to anomaly events and attempts correlation with recent deployments
    @KafkaListener(
            topics = "${devtrust.kafka.topics.anomalies}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAnomaly(@Payload AnomalyEvent anomaly) {
        log.info("Received anomaly: service={} metric={} severity={}",
                anomaly.getServiceName(), anomaly.getMetricName(), anomaly.getSeverity());

        DeploymentEvent recentDeploy = recentDeployments.get(anomaly.getServiceName());

        if (recentDeploy == null) {
            log.debug("No recent deployment found for service [{}] — anomaly not correlated", anomaly.getServiceName());
            return;
        }

        long deltaSeconds = anomaly.getTimestamp().getEpochSecond()
                - recentDeploy.getTimestamp().getEpochSecond();

        boolean withinWindow = deltaSeconds >= 0 && deltaSeconds <= correlationWindowSeconds;

        if (!withinWindow) {
            log.debug("Anomaly on [{}] is {}s after deployment — outside {} window",
                    anomaly.getServiceName(), deltaSeconds, correlationWindowSeconds);
            return;
        }

        // Correlation found — compute confidence and emit incident
        double confidence = computeConfidence(deltaSeconds, anomaly.getSeverity());

        CorrelatedIncident incident = CorrelatedIncident.builder()
                .incidentId(UUID.randomUUID().toString())
                .deployment(recentDeploy)
                .anomaly(anomaly)
                .deltaSeconds(deltaSeconds)
                .confidenceScore(confidence)
                .detectedAt(Instant.now())
                .build();

        kafkaTemplate.send(incidentsTopic, anomaly.getServiceName(), incident);

        log.warn("""
                CORRELATION DETECTED
                  Service    : {}
                  Commit     : {} by {}
                  Anomaly    : {} = {:.1f} ({})
                  Delta      : {}s after deploy
                  Confidence : {:.0f}%
                """,
                anomaly.getServiceName(),
                recentDeploy.getCommitId(), recentDeploy.getAuthor(),
                anomaly.getMetricName(), anomaly.getValue(), anomaly.getSeverity(),
                deltaSeconds,
                confidence * 100);
    }

    // Closer in time + higher severity = higher confidence
    private double computeConfidence(long deltaSeconds, String severity) {
        double timeScore = 1.0 - ((double) deltaSeconds / correlationWindowSeconds);

        double severityScore = switch (severity) {
            case "CRITICAL" -> 1.0;
            case "HIGH"     -> 0.8;
            case "MEDIUM"   -> 0.5;
            default         -> 0.3;
        };

        return Math.round(((timeScore * 0.6) + (severityScore * 0.4)) * 100.0) / 100.0;
    }
}