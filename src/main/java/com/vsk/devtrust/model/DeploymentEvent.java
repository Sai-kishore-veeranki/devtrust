package com.vsk.devtrust.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentEvent {
    private String deploymentId;
    private String commitId;
    private String author;
    private String serviceName;
    private String environment;       // "production", "staging"
    private List<String> changedFiles;
    private Instant timestamp;
}