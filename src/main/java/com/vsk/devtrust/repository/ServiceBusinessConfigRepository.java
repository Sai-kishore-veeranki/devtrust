package com.vsk.devtrust.repository;

import com.vsk.devtrust.entity.ServiceBusinessConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceBusinessConfigRepository extends JpaRepository<ServiceBusinessConfig, Long> {
    Optional<ServiceBusinessConfig> findByServiceName(String serviceName);
}