package com.vsk.devtrust.runbook;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RunbookService {

    private static final Pattern NON_ALNUMERIC = Pattern.compile("[^a-z0-9]+");

    private final RunbookRepository repository;

    public List<RunbookEntity> listAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<RunbookEntity> listByService(String serviceName) {
        return repository.findByServiceNameOrderByCreatedAtDesc(serviceName);
    }

    public RunbookEntity getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Runbook not found with id: " + id));
    }

    @Transactional
    public RunbookEntity create(RunbookRequest request) {
        validate(request);

        RunbookEntity entity = RunbookEntity.builder()
                .slug(buildSlug(request.getTitle()))
                .title(request.getTitle().trim())
                .serviceName(request.getServiceName().trim())
                .severity(request.getSeverity())
                .trigger(request.getTrigger().trim())
                .summary(request.getSummary() == null ? "" : request.getSummary().trim())
                .active(true)
                .steps(normalizeSteps(request.getSteps()))
                .owners(normalizeOwners(request.getOwners()))
                .build();

        return repository.save(entity);
    }

    @Transactional
    public RunbookEntity update(Long id, RunbookRequest request) {
        RunbookEntity existing = getById(id);
        validate(request);

        existing.setTitle(request.getTitle().trim());
        existing.setServiceName(request.getServiceName().trim());
        existing.setSeverity(request.getSeverity());
        existing.setTrigger(request.getTrigger().trim());
        existing.setSummary(request.getSummary() == null ? "" : request.getSummary().trim());
        existing.setSteps(normalizeSteps(request.getSteps()));
        existing.setOwners(normalizeOwners(request.getOwners()));
        existing.setSlug(buildSlug(request.getTitle(), existing.getSlug()));
        existing.setUpdatedAt(Instant.now());

        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Runbook not found with id: " + id);
        }
        repository.deleteById(id);
    }

    public RunbookExecutionResult execute(Long id) {
        RunbookEntity runbook = getById(id);

        List<String> executedSteps = runbook.getSteps().isEmpty()
                ? List.of("No steps are defined for this runbook yet. Add a recovery checklist before execution.")
                : runbook.getSteps().stream()
                .map(RunbookStep::getTitle)
                .toList();

        return new RunbookExecutionResult(
                runbook.getId(),
                runbook.getTitle(),
                "READY",
                "The runbook has been prepared for operator review and execution.",
                executedSteps,
                Instant.now(),
                Instant.now()
        );
    }

    @Transactional
    public void seedDefaults() {
        if (repository.count() > 0) {
            return;
        }

        List<RunbookEntity> defaults = List.of(
                RunbookEntity.builder()
                        .slug("payment-service-latency-recovery")
                        .title("Payment service latency recovery")
                        .serviceName("payment-service")
                        .severity(RunbookSeverity.SEV1)
                        .trigger("High latency or checkout failures")
                        .summary("Protect revenue by verifying dependency health, rolling back recent changes, and validating the checkout path.")
                        .steps(List.of(
                                RunbookStep.builder()
                                        .title("Confirm incident scope")
                                        .description("Check the incident timeline and verify whether checkout or payment APIs are degraded.")
                                        .action("Review the incident card, recent deployments, and the last 15 minutes of payment API latency trend.")
                                        .owner("oncall")
                                        .expectedOutcome("Incident scope is confirmed and the blast radius is documented.")
                                        .build(),
                                RunbookStep.builder()
                                        .title("Checkpoint dependencies")
                                        .description("Review auth, database, and gateway health to isolate whether an upstream dependency is failing.")
                                        .action("Validate database connection pool, Redis saturation, and auth-service health before changing application code.")
                                        .owner("platform")
                                        .expectedOutcome("No dependency-driven outage remains active.")
                                        .build(),
                                RunbookStep.builder()
                                        .title("Revert suspect release")
                                        .description("Rollback the most recent payment-service change if latency spikes immediately after deployment.")
                                        .action("Use the deployment history to revert or disable the last release and confirm traffic recovers within one verification cycle.")
                                        .owner("service-owner")
                                        .expectedOutcome("Gateway and checkout latency return to normal thresholds.")
                                        .build()
                        ))
                        .owners(new LinkedHashSet<>(List.of("ops@company.com", "payment-team@company.com")))
                        .build(),
                RunbookEntity.builder()
                        .slug("auth-service-token-storm-recovery")
                        .title("Auth service token storm recovery")
                        .serviceName("auth-service")
                        .severity(RunbookSeverity.SEV1)
                        .trigger("Token validation errors or 401 spikes")
                        .summary("Protect login traffic and reduce user impact by scaling auth resources and validating token configuration.")
                        .steps(List.of(
                                RunbookStep.builder()
                                        .title("Review auth error pattern")
                                        .description("Confirm if the issue is due to invalid tokens, secret rotation, or downstream database lock contention.")
                                        .action("Check the auth service logs, token validation errors, and recent configuration changes for the last hour.")
                                        .owner("identity")
                                        .expectedOutcome("Root cause is narrowed to a config or dependency issue.")
                                        .build(),
                                RunbookStep.builder()
                                        .title("Scale and isolate the auth tier")
                                        .description("Increase capacity and stop any traffic build-up that is amplifying the issue.")
                                        .action("Increase replicas or block suspicious traffic patterns while preserving authentication availability for active sessions.")
                                        .owner("platform")
                                        .expectedOutcome("Service returns to healthy throughput and queue depth is stable.")
                                        .build(),
                                RunbookStep.builder()
                                        .title("Validate fix with user journey")
                                        .description("Verify that login, refresh, and session validation succeed for representative users.")
                                        .action("Run a smoke test across login, token refresh, and protected route access before closing the incident.")
                                        .owner("service-owner")
                                        .expectedOutcome("Authentication path is healthy and no new user-facing errors appear.")
                                        .build()
                        ))
                        .owners(new LinkedHashSet<>(List.of("security@company.com", "auth-team@company.com")))
                        .build()
        );

        repository.saveAll(defaults);
    }

    private void validate(RunbookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Runbook request is required.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required.");
        }
        if (request.getServiceName() == null || request.getServiceName().isBlank()) {
            throw new IllegalArgumentException("serviceName is required.");
        }
        if (request.getTrigger() == null || request.getTrigger().isBlank()) {
            throw new IllegalArgumentException("trigger is required.");
        }
        if (request.getSeverity() == null) {
            throw new IllegalArgumentException("severity is required.");
        }
    }

    private String buildSlug(String title) {
        return buildSlug(title, null);
    }

    private String buildSlug(String title, String existingSlug) {
        String base = NON_ALNUMERIC.matcher(title.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        String slug = base.replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            slug = "runbook";
        }
        if (existingSlug != null && existingSlug.equals(slug)) {
            return existingSlug;
        }
        if (!repository.existsBySlug(slug)) {
            return slug;
        }
        return slug + "-" + System.currentTimeMillis();
    }

    private List<RunbookStep> normalizeSteps(List<RunbookStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return new ArrayList<>();
        }

        return steps.stream()
                .filter(Objects::nonNull)
                .map(step -> RunbookStep.builder()
                        .title(step.getTitle() == null ? "Untitled step" : step.getTitle().trim())
                        .description(step.getDescription() == null ? "" : step.getDescription().trim())
                        .action(step.getAction() == null ? "Review the service state and continue with the recovery plan." : step.getAction().trim())
                        .owner(step.getOwner() == null || step.getOwner().isBlank() ? "oncall" : step.getOwner().trim())
                        .expectedOutcome(step.getExpectedOutcome() == null || step.getExpectedOutcome().isBlank()
                                ? "Service recovered and validated"
                                : step.getExpectedOutcome().trim())
                        .build())
                .collect(Collectors.toList());
    }

    private LinkedHashSet<String> normalizeOwners(Set<String> owners) {
        if (owners == null || owners.isEmpty()) {
            return new LinkedHashSet<>();
        }

        return owners.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static class NoSuchElementException extends RuntimeException {
        public NoSuchElementException(String message) {
            super(message);
        }
    }
}
