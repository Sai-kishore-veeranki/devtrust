package com.vsk.devtrust.repository;

import com.vsk.devtrust.entity.ServiceNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceNodeRepository extends JpaRepository<ServiceNode, Long> {
    Optional<ServiceNode> findByServiceName(String serviceName);
    List<ServiceNode> findAllByOrderByTotalIncidentsDesc();
}