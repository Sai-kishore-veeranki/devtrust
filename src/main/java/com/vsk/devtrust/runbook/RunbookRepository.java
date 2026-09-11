package com.vsk.devtrust.runbook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RunbookRepository extends JpaRepository<RunbookEntity, Long> {

    List<RunbookEntity> findAllByOrderByCreatedAtDesc();

    List<RunbookEntity> findByServiceNameOrderByCreatedAtDesc(String serviceName);

    Optional<RunbookEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
