package com.vsk.devtrust.service;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.entity.ServiceDependency;
import com.vsk.devtrust.entity.ServiceNode;
import com.vsk.devtrust.repository.IncidentRepository;
import com.vsk.devtrust.repository.ServiceDependencyRepository;
import com.vsk.devtrust.repository.ServiceNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceGraphService {

    private final ServiceNodeRepository serviceNodeRepository;
    private final ServiceDependencyRepository dependencyRepository;
    private final IncidentRepository incidentRepository;

    // Called every time a new incident is detected —
    // auto-registers the service and updates its health
    public void registerOrUpdateService(IncidentEntity incident) {
        ServiceNode node = serviceNodeRepository
                .findByServiceName(incident.getServiceName())
                .orElse(ServiceNode.builder()
                        .serviceName(incident.getServiceName())
                        .totalIncidents(0)
                        .status("UNKNOWN")
                        .build());

        node.setTotalIncidents(node.getTotalIncidents() + 1);
        node.setLastIncidentAt(incident.getDetectedAt());
        node.setLastUpdated(Instant.now());
        node.setStatus(computeHealthStatus(node.getTotalIncidents(),
                incident.getDetectedAt()));

        serviceNodeRepository.save(node);
        log.info("Service node updated: {} status={} incidents={}",
                node.getServiceName(), node.getStatus(), node.getTotalIncidents());
    }

    public void addDependency(String fromService, String toService, String type) {
        if (dependencyRepository.existsByFromServiceAndToService(fromService, toService)) {
            log.info("Dependency already exists: {} -> {}", fromService, toService);
            return;
        }

        dependencyRepository.save(ServiceDependency.builder()
                .fromService(fromService)
                .toService(toService)
                .dependencyType(type)
                .build());

        // Auto-register both services as nodes if they don't exist yet
        ensureNodeExists(fromService);
        ensureNodeExists(toService);

        log.info("Dependency added: {} -> {} ({})", fromService, toService, type);
    }

    public Map<String, Object> getGraphData() {
        List<ServiceNode> nodes = serviceNodeRepository.findAllByOrderByTotalIncidentsDesc();
        List<ServiceDependency> edges = dependencyRepository.findAll();

        return Map.of(
                "nodes", nodes.stream().map(n -> Map.of(
                        "id", n.getServiceName(),
                        "label", n.getServiceName(),
                        "status", n.getStatus(),
                        "totalIncidents", n.getTotalIncidents(),
                        "lastIncidentAt", n.getLastIncidentAt() != null
                                ? n.getLastIncidentAt().toString() : "never",
                        "tier", n.getTier() != null ? n.getTier() : "UNKNOWN"
                )).toList(),
                "edges", edges.stream().map(e -> Map.of(
                        "from", e.getFromService(),
                        "to", e.getToService(),
                        "type", e.getDependencyType()
                )).toList()
        );
    }

    private String computeHealthStatus(int totalIncidents, Instant lastIncidentAt) {
        long hoursSinceLastIncident = ChronoUnit.HOURS.between(lastIncidentAt, Instant.now());

        if (hoursSinceLastIncident < 1) return "CRITICAL";
        if (hoursSinceLastIncident < 24) return "DEGRADED";
        if (totalIncidents > 10) return "DEGRADED";
        return "HEALTHY";
    }

    private void ensureNodeExists(String serviceName) {
        if (serviceNodeRepository.findByServiceName(serviceName).isEmpty()) {
            serviceNodeRepository.save(ServiceNode.builder()
                    .serviceName(serviceName)
                    .totalIncidents(0)
                    .status("UNKNOWN")
                    .lastUpdated(Instant.now())
                    .build());
        }
    }

    public void updateServiceStatus(String serviceName, String status) {
        serviceNodeRepository.findByServiceName(serviceName).ifPresent(node -> {
            node.setStatus(status);
            node.setLastUpdated(Instant.now());
            serviceNodeRepository.save(node);
            log.info("Service status manually updated: {} -> {}", serviceName, status);
        });
    }
}