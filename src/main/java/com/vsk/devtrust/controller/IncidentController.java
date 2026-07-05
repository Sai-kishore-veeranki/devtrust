package com.vsk.devtrust.controller;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.model.BlastRadius;
import com.vsk.devtrust.repository.IncidentRepository;
import com.vsk.devtrust.service.BlastRadiusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final BlastRadiusService blastRadiusService;

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



    @PatchMapping("/{incidentId}/resolve")
    public ResponseEntity<IncidentEntity> resolveIncident(@PathVariable String incidentId) {
        return incidentRepository.findAll().stream()
                .filter(i -> i.getIncidentId().equals(incidentId))
                .findFirst()
                .map(incident -> {
                    Instant resolvedAt = Instant.now();
                    incident.setStatus("RESOLVED");
                    incident.setResolvedAt(resolvedAt);

                    // Recompute with real total duration now that it's resolved
                    BlastRadius finalBlastRadius = blastRadiusService.compute(incident, resolvedAt);
                    incident.setEstimatedRevenueLost(finalBlastRadius.getEstimatedRevenueLost());
                    incident.setEstimatedUsersAffected(finalBlastRadius.getEstimatedUsersAffected());
                    incident.setDurationMinutes(finalBlastRadius.getDurationMinutes());
                    incident.setSlaBreached(finalBlastRadius.isSlaBreached());
                    incident.setCostSummary(finalBlastRadius.getCostSummary());

                    return ResponseEntity.ok(incidentRepository.save(incident));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}