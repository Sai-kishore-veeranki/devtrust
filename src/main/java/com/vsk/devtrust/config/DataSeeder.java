package com.vsk.devtrust.config;

import com.vsk.devtrust.entity.ServiceBusinessConfig;
import com.vsk.devtrust.repository.ServiceBusinessConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final ServiceBusinessConfigRepository configRepository;

    @Override
    public void run(ApplicationArguments args) {
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
                        .build()
        );

        configRepository.saveAll(defaults);
        log.info("Seeded {} default service business configs", defaults.size());
    }
}