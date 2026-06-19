package com.vsk.devtrust.consumer;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.model.AnomalyEvent;
import com.vsk.devtrust.model.CorrelatedIncident;
import com.vsk.devtrust.model.DeploymentEvent;
import com.vsk.devtrust.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorrelationEngine {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, DeploymentEvent> deploymentRedisTemplate;
    private final IncidentRepository incidentRepository;
    private final SimpMessagingTemplate messagingTemplate;   // NEW

    @Value("${devtrust.kafka.topics.incidents}")
    private String incidentsTopic;

    @Value("${devtrust.correlation.window-seconds}")
    private long correlationWindowSeconds;

    private static final String REDIS_KEY_PREFIX = "deploy:";

    @KafkaListener(
            topics = "${devtrust.kafka.topics.deployments}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onDeployment(@Payload DeploymentEvent event) {
        log.info("Deployment received: service={} commit={} author={}",
                event.getServiceName(), event.getCommitId(), event.getAuthor());

        String key = REDIS_KEY_PREFIX + event.getServiceName();
        deploymentRedisTemplate.opsForValue()
                .set(key, event, correlationWindowSeconds, TimeUnit.SECONDS);
    }

    @KafkaListener(
            topics = "${devtrust.kafka.topics.anomalies}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onAnomaly(@Payload AnomalyEvent anomaly) {
        log.info("Anomaly received: service={} metric={} severity={}",
                anomaly.getServiceName(), anomaly.getMetricName(), anomaly.getSeverity());

        String key = REDIS_KEY_PREFIX + anomaly.getServiceName();
        DeploymentEvent recentDeploy = deploymentRedisTemplate.opsForValue().get(key);

        if (recentDeploy == null) {
            log.debug("No recent deployment in window for service [{}]", anomaly.getServiceName());
            return;
        }

        long deltaSeconds = anomaly.getTimestamp().getEpochSecond()
                - recentDeploy.getTimestamp().getEpochSecond();

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

        IncidentEntity entity = IncidentEntity.builder()
                .incidentId(incident.getIncidentId())
                .serviceName(anomaly.getServiceName())
                .commitId(recentDeploy.getCommitId())
                .author(recentDeploy.getAuthor())
                .metricName(anomaly.getMetricName())
                .anomalyValue(anomaly.getValue())
                .threshold(anomaly.getThreshold())
                .severity(anomaly.getSeverity())
                .deltaSeconds(deltaSeconds)
                .confidenceScore(confidence)
                .detectedAt(incident.getDetectedAt())
                .build();

        incidentRepository.save(entity);

        // NEW — push live to any subscribed React client
        messagingTemplate.convertAndSend("/topic/incidents", entity);

        log.warn("CORRELATION DETECTED | service={} commit={} author={} metric={} severity={} delta={}s confidence={}%",
                anomaly.getServiceName(), recentDeploy.getCommitId(), recentDeploy.getAuthor(),
                anomaly.getMetricName(), anomaly.getSeverity(), deltaSeconds,
                Math.round(confidence * 100));
    }

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