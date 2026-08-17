package com.vsk.devtrust.repository;

import com.vsk.devtrust.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
    List<IncidentEntity> findByServiceNameOrderByDetectedAtDesc(String serviceName);
    List<IncidentEntity> findTop20ByOrderByDetectedAtDesc();
    boolean existsByCorrelationKey(String correlationKey);

    List<IncidentEntity> findByDetectedAtAfterOrderByDetectedAtDesc(Instant since);

    Optional<IncidentEntity> findByIncidentId(String incidentId);

    long countByServiceNameAndDetectedAtAfter(String serviceName, Instant since);
}