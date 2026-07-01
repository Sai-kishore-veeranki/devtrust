package com.vsk.devtrust.repository;

import com.vsk.devtrust.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
    List<IncidentEntity> findByServiceNameOrderByDetectedAtDesc(String serviceName);
    List<IncidentEntity> findTop20ByOrderByDetectedAtDesc();
    boolean existsByCorrelationKey(String correlationKey);

    List<IncidentEntity> findByDetectedAtAfterOrderByDetectedAtDesc(Instant since);
}