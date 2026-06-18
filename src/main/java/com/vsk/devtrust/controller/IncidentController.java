package com.vsk.devtrust.controller;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentRepository incidentRepository;

    // GET /api/incidents — latest 20 incidents
    @GetMapping
    public ResponseEntity<List<IncidentEntity>> getLatestIncidents() {
        return ResponseEntity.ok(incidentRepository.findTop20ByOrderByDetectedAtDesc());
    }

    // GET /api/incidents/{id}
    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentEntity> getIncident(@PathVariable String incidentId) {
        return incidentRepository.findAll().stream()
                .filter(i -> i.getIncidentId().equals(incidentId))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/incidents/service/{serviceName}
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<IncidentEntity>> getByService(@PathVariable String serviceName) {
        return ResponseEntity.ok(
                incidentRepository.findByServiceNameOrderByDetectedAtDesc(serviceName));
    }
}