package com.vsk.devtrust.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "service_nodes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serviceName;

    private String tier;
    private String status; // HEALTHY, DEGRADED, CRITICAL, UNKNOWN
    private Instant lastIncidentAt;
    private int totalIncidents;
    private Instant lastUpdated;
}