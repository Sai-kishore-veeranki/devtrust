package com.vsk.devtrust.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "incidents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String incidentId;

    private String serviceName;
    private String commitId;
    private String author;
    private String metricName;
    private double anomalyValue;
    private double threshold;
    private String severity;
    private long deltaSeconds;
    private double confidenceScore;
    private Instant detectedAt;
}