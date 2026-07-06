package com.vsk.devtrust.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "service_dependencies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "payment-service depends on auth-service"
    private String fromService;
    private String toService;
    private String dependencyType; // HARD (outage propagates), SOFT (degraded only)
}