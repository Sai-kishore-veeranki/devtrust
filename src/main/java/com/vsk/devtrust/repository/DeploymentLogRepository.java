package com.vsk.devtrust.repository;

import com.vsk.devtrust.entity.DeploymentLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DeploymentLogRepository extends JpaRepository<DeploymentLogEntity, Long> {
    long countByDeployedAtAfter(Instant since);
    List<DeploymentLogEntity> findByDeployedAtAfterOrderByDeployedAtDesc(Instant since);
}
