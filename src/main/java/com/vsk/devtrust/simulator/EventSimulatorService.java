//package com.vsk.devtrust.simulator;
//
//import com.vsk.devtrust.model.AnomalyEvent;
//import com.vsk.devtrust.model.DeploymentEvent;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.ThreadLocalRandom;
//
//@Slf4j
//@Service
//@EnableScheduling
//@RequiredArgsConstructor
//public class EventSimulatorService {
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    @Value("${devtrust.kafka.topics.deployments}")
//    private String deploymentsTopic;
//
//    @Value("${devtrust.kafka.topics.anomalies}")
//    private String anomaliesTopic;
//
//    // Simulates a deployment every 30 seconds
//    @Scheduled(fixedDelay = 30_000)
//    public void simulateDeployment() {
//        String[] services = {"payment-service", "auth-service", "order-service", "notification-service"};
//        String[] authors  = {"alice", "bob", "carlos", "diana"};
//
//        String service = services[ThreadLocalRandom.current().nextInt(services.length)];
//
//        DeploymentEvent event = DeploymentEvent.builder()
//                .deploymentId(UUID.randomUUID().toString())
//                .commitId("abc" + ThreadLocalRandom.current().nextInt(10000, 99999))
//                .author(authors[ThreadLocalRandom.current().nextInt(authors.length)])
//                .serviceName(service)
//                .environment("production")
//                .changedFiles(List.of("src/main/PaymentProcessor.java", "src/main/Config.java"))
//                .timestamp(Instant.now())
//                .build();
//
//        kafkaTemplate.send(deploymentsTopic, service, event);
//        log.info("Deployed [{}] to {} by {}", event.getCommitId(), service, event.getAuthor());
//    }
//
//    // Simulates an anomaly every 20 seconds
//    @Scheduled(fixedDelay = 20_000, initialDelay = 10_000)
//    public void simulateAnomaly() {
//        String[] services = {"payment-service", "auth-service", "order-service", "notification-service"};
//        String[] metrics  = {"p99_latency_ms", "error_rate_percent", "cpu_usage_percent"};
//        String[] severity = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
//
//        String service = services[ThreadLocalRandom.current().nextInt(services.length)];
//        String metric  = metrics[ThreadLocalRandom.current().nextInt(metrics.length)];
//
//        double value     = ThreadLocalRandom.current().nextDouble(80, 200);
//        double threshold = 75.0;
//
//        AnomalyEvent event = AnomalyEvent.builder()
//                .anomalyId(UUID.randomUUID().toString())
//                .serviceName(service)
//                .metricName(metric)
//                .value(value)
//                .threshold(threshold)
//                .severity(severity[ThreadLocalRandom.current().nextInt(severity.length)])
//                .timestamp(Instant.now())
//                .build();
//
//        kafkaTemplate.send(anomaliesTopic, service, event);
//        log.info("Anomaly on [{}]: {} = {:.1f} (threshold: {})", service, metric, value, threshold);
//    }
//}