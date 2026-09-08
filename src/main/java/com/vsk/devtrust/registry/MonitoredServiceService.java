package com.vsk.devtrust.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MonitoredServiceService {

    private static final Set<String> VALID_TIERS = Set.of("TIER_1", "TIER_2", "TIER_3");

    private final MonitoredServiceRepository repository;

    public List<MonitoredService> listAll() {
        return repository.findAll();
    }

    public MonitoredService get(String serviceName) {
        return repository.findByServiceName(serviceName)
                .orElseThrow(() -> new NoSuchElementException("No service registered as: " + serviceName));
    }

    public MonitoredService create(MonitoredService input) {
        validate(input);
        if (repository.existsByServiceName(input.getServiceName())) {
            throw new IllegalStateException("A service named '" + input.getServiceName() + "' is already registered.");
        }

        input.setId(null);
        input.setCreatedAt(Instant.now());
        input.setUpdatedAt(Instant.now());
        return repository.save(input);
    }

    public MonitoredService update(String serviceName, MonitoredService updates) {
        MonitoredService existing = get(serviceName);

        if (updates.getRepoUrl() != null) existing.setRepoUrl(updates.getRepoUrl());
        if (updates.getWebhookSecret() != null) existing.setWebhookSecret(updates.getWebhookSecret());
        if (updates.getPrometheusTarget() != null) existing.setPrometheusTarget(updates.getPrometheusTarget());
        if (updates.getLatencyThresholdMs() != null) existing.setLatencyThresholdMs(updates.getLatencyThresholdMs());
        if (updates.getHeapThresholdPercent() != null) existing.setHeapThresholdPercent(updates.getHeapThresholdPercent());
        if (updates.getBusinessTier() != null) {
            validateTier(updates.getBusinessTier());
            existing.setBusinessTier(updates.getBusinessTier());
        }
        existing.setActive(updates.isActive());
        existing.setUpdatedAt(Instant.now());

        return repository.save(existing);
    }

    public void delete(String serviceName) {
        MonitoredService existing = get(serviceName);
        repository.delete(existing);
    }

    private void validate(MonitoredService input) {
        if (input.getServiceName() == null || input.getServiceName().isBlank()) {
            throw new IllegalArgumentException("serviceName is required.");
        }
        if (input.getLatencyThresholdMs() != null && input.getLatencyThresholdMs() <= 0) {
            throw new IllegalArgumentException("latencyThresholdMs must be positive.");
        }
        if (input.getHeapThresholdPercent() != null
                && (input.getHeapThresholdPercent() <= 0 || input.getHeapThresholdPercent() > 100)) {
            throw new IllegalArgumentException("heapThresholdPercent must be between 0 and 100.");
        }
        if (input.getBusinessTier() != null) {
            validateTier(input.getBusinessTier());
        }
    }

    private void validateTier(String tier) {
        if (!VALID_TIERS.contains(tier)) {
            throw new IllegalArgumentException("businessTier must be one of " + VALID_TIERS);
        }
    }

    public static class NoSuchElementException extends RuntimeException {
        public NoSuchElementException(String message) {
            super(message);
        }
    }
}
