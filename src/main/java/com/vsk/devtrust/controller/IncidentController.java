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
    public ResponseEntity<List<com.vsk.devtrust.dto.IncidentDto>> getLatestIncidents() {
        var incidents = incidentRepository.findTop20ByOrderByDetectedAtDesc();
        var dtos = incidents.stream().map(com.vsk.devtrust.dto.IncidentMapper::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    // GET /api/incidents/{id}
    @GetMapping("/{incidentId}")
    public ResponseEntity<com.vsk.devtrust.dto.IncidentDto> getIncident(@PathVariable String incidentId) {
        return incidentRepository.findByIncidentId(incidentId)
                .map(com.vsk.devtrust.dto.IncidentMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/incidents/service/{serviceName}
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<com.vsk.devtrust.dto.IncidentDto>> getByService(@PathVariable String serviceName) {
        var incidents = incidentRepository.findByServiceNameOrderByDetectedAtDesc(serviceName);
        var dtos = incidents.stream().map(com.vsk.devtrust.dto.IncidentMapper::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/{incidentId}/resolve")
    public ResponseEntity<com.vsk.devtrust.dto.IncidentDto> resolveIncident(@PathVariable String incidentId) {
        return incidentRepository.findByIncidentId(incidentId)
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

                    IncidentEntity saved = incidentRepository.save(incident);
                    return ResponseEntity.ok(com.vsk.devtrust.dto.IncidentMapper.toDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}