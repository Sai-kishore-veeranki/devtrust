package com.vsk.devtrust.config;

import com.vsk.devtrust.entity.ServiceBusinessConfig;
import com.vsk.devtrust.entity.ServiceDependency;
import com.vsk.devtrust.entity.ServiceNode;
import com.vsk.devtrust.repository.ServiceBusinessConfigRepository;
import com.vsk.devtrust.repository.ServiceDependencyRepository;
import com.vsk.devtrust.repository.ServiceNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final ServiceBusinessConfigRepository configRepository;
    private final ServiceDependencyRepository dependencyRepository;
    private final ServiceNodeRepository serviceNodeRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedBusinessConfigs();
        seedServiceGraph();
    }

    private void seedBusinessConfigs() {
        if (configRepository.count() > 0) return;

        List<ServiceBusinessConfig> defaults = List.of(
                ServiceBusinessConfig.builder()
                        .serviceName("devtrust-test-repo")
                        .revenuePerMinute(150.0)
                        .activeUsersPerMinute(500.0)
                        .slaThresholdMinutes(15)
                        .tier("TIER_1")
                        .build(),
                ServiceBusinessConfig.builder()
                        .serviceName("payment-service")
                        .revenuePerMinute(800.0)
                        .activeUsersPerMinute(1200.0)
                        .slaThresholdMinutes(5)
                        .tier("TIER_1")
                        .build(),
                ServiceBusinessConfig.builder()
                        .serviceName("auth-service")
                        .revenuePerMinute(400.0)
                        .activeUsersPerMinute(2000.0)
                        .slaThresholdMinutes(10)
                        .tier("TIER_1")
                        .build(),
                ServiceBusinessConfig.builder()
                        .serviceName("order-service")
                        .revenuePerMinute(600.0)
                        .activeUsersPerMinute(800.0)
                        .slaThresholdMinutes(10)
                        .tier("TIER_2")
                        .build(),
                ServiceBusinessConfig.builder()
                        .serviceName("notification-service")
                        .revenuePerMinute(50.0)
                        .activeUsersPerMinute(300.0)
                        .slaThresholdMinutes(30)
                        .tier("TIER_2")
                        .build()
        );

        configRepository.saveAll(defaults);
        log.info("Seeded {} default service business configs", defaults.size());
    }

    private void seedServiceGraph() {
        if (serviceNodeRepository.count() > 0) return;

        // Seed service nodes
        List<ServiceNode> nodes = List.of(
                ServiceNode.builder()
                        .serviceName("devtrust-test-repo")
                        .tier("TIER_1")
                        .status("HEALTHY")
                        .totalIncidents(0)
                        .lastUpdated(Instant.now())
                        .build(),
                ServiceNode.builder()
                        .serviceName("payment-service")
                        .tier("TIER_1")
                        .status("HEALTHY")
                        .totalIncidents(0)
                        .lastUpdated(Instant.now())
                        .build(),
                ServiceNode.builder()
                        .serviceName("auth-service")
                        .tier("TIER_1")
                        .status("HEALTHY")
                        .totalIncidents(0)
                        .lastUpdated(Instant.now())
                        .build(),
                ServiceNode.builder()
                        .serviceName("order-service")
                        .tier("TIER_2")
                        .status("HEALTHY")
                        .totalIncidents(0)
                        .lastUpdated(Instant.now())
                        .build(),
                ServiceNode.builder()
                        .serviceName("notification-service")
                        .tier("TIER_2")
                        .status("HEALTHY")
                        .totalIncidents(0)
                        .lastUpdated(Instant.now())
                        .build()
        );

        serviceNodeRepository.saveAll(nodes);
        log.info("Seeded {} service nodes", nodes.size());

        if (dependencyRepository.count() > 0) return;

        // Seed realistic microservice dependency graph
        List<ServiceDependency> deps = List.of(
                ServiceDependency.builder()
                        .fromService("devtrust-test-repo")
                        .toService("auth-service")
                        .dependencyType("HARD")
                        .build(),
                ServiceDependency.builder()
                        .fromService("devtrust-test-repo")
                        .toService("payment-service")
                        .dependencyType("HARD")
                        .build(),
                ServiceDependency.builder()
                        .fromService("order-service")
                        .toService("payment-service")
                        .dependencyType("HARD")
                        .build(),
                ServiceDependency.builder()
                        .fromService("order-service")
                        .toService("notification-service")
                        .dependencyType("SOFT")
                        .build(),
                ServiceDependency.builder()
                        .fromService("payment-service")
                        .toService("auth-service")
                        .dependencyType("HARD")
                        .build(),
                ServiceDependency.builder()
                        .fromService("notification-service")
                        .toService("auth-service")
                        .dependencyType("SOFT")
                        .build()
        );

        dependencyRepository.saveAll(deps);
        log.info("Seeded {} service dependencies", deps.size());
    }
}