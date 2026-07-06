package com.vsk.devtrust.repository;

import com.vsk.devtrust.entity.ServiceDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceDependencyRepository extends JpaRepository<ServiceDependency, Long> {
    List<ServiceDependency> findByFromServiceOrToService(String fromService, String toService);
    boolean existsByFromServiceAndToService(String fromService, String toService);
}