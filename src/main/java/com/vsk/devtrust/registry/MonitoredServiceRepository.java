package com.vsk.devtrust.registry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, Long> {
    Optional<MonitoredService> findByServiceName(String serviceName);
    boolean existsByServiceName(String serviceName);
    List<MonitoredService> findByActiveTrue();
}
