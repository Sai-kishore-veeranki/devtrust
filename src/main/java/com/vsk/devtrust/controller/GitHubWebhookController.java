package com.vsk.devtrust.controller;

import com.vsk.devtrust.model.DeploymentEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/webhooks/github")
@RequiredArgsConstructor
public class GitHubWebhookController {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${devtrust.github.webhook-secret}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) {

        log.info("Received GitHub webhook: event={}", eventType);

        if (!isValidSignature(rawPayload, signature)) {
            log.warn("Invalid or missing webhook signature — rejecting payload");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        if (!"push".equals(eventType)) {
            log.debug("Ignoring non-push event: {}", eventType);
            return ResponseEntity.ok("ignored");
        }

        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            DeploymentEvent event = mapToDeploymentEvent(payload);

            kafkaTemplate.send("devtrust.deployments", event.getServiceName(), event);

            log.info("Deployment event published: service={} commit={} author={}",
                    event.getServiceName(), event.getCommitId(), event.getAuthor());

            return ResponseEntity.ok("processed");

        } catch (Exception e) {
            log.error("Failed to process GitHub webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("error processing payload");
        }
    }

    private boolean isValidSignature(String payload, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

            byte[] computedHashBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHash = "sha256=" + HexFormat.of().formatHex(computedHashBytes);

            return constantTimeEquals(computedHash, signatureHeader);

        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    // Prevents timing-attack vulnerabilities when comparing signatures
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private DeploymentEvent mapToDeploymentEvent(JsonNode payload) {
        String repoName = payload.path("repository").path("name").asText("unknown-service");
        String pusher = payload.path("pusher").path("name").asText("unknown-author");
        String commitSha = payload.path("after").asText("");

        List<String> changedFiles = new ArrayList<>();
        JsonNode commits = payload.path("commits");
        if (commits.isArray()) {
            for (JsonNode commit : commits) {
                commit.path("modified").forEach(f -> changedFiles.add(f.asText()));
                commit.path("added").forEach(f -> changedFiles.add(f.asText()));
            }
        }

        return DeploymentEvent.builder()
                .deploymentId(UUID.randomUUID().toString())
                .commitId(commitSha.length() >= 7 ? commitSha.substring(0, 7) : commitSha)
                .author(pusher)
                .serviceName(repoName)
                .environment("production")
                .changedFiles(changedFiles)
                .timestamp(Instant.now())
                .build();
    }
}