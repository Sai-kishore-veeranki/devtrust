package com.vsk.devtrust.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "service_business_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBusinessConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serviceName;

    // Revenue this service generates per minute when healthy
    private double revenuePerMinute;

    // Average active users hitting this service per minute
    private double activeUsersPerMinute;

    // SLA threshold in minutes — beyond this, SLA is breached
    private int slaThresholdMinutes;

    // Criticality tier: TIER_1 (customer-facing), TIER_2 (internal), TIER_3 (background)
    private String tier;
}