package com.vsk.devtrust.warroom;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Wires IncidentWarRoomService to real data — this class didn't exist
 * before, which is why the war-room feature was unreachable from any
 * endpoint despite being fully built.
 */
@RestController
@RequestMapping("/api/warroom")
@RequiredArgsConstructor
public class WarRoomController {

    private final IncidentRepository incidentRepository;
    private final IncidentWarRoomService warRoomService;

    @GetMapping("/summary")
    public IncidentWarRoomSummary getSummary() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);

        List<IncidentSummaryInput> activeIncidents = incidentRepository
                .findByDetectedAtAfterOrderByDetectedAtDesc(since)
                .stream()
                .filter(i -> "OPEN".equals(i.getStatus()))
                .map(this::toSummaryInput)
                .toList();

        return warRoomService.buildSummary(activeIncidents);
    }

    private IncidentSummaryInput toSummaryInput(IncidentEntity incident) {
        return new IncidentSummaryInput(
                incident.getIncidentId(),
                incident.getServiceName(),
                incident.getSeverity(),
                incident.getEstimatedRevenueLost() != null ? incident.getEstimatedRevenueLost() : 0.0,
                incident.getEstimatedUsersAffected() != null ? incident.getEstimatedUsersAffected().longValue() : 0L,
                incident.getAnomalyValue(),
                incident.getRootCauseAnalysis() != null
                        ? incident.getRootCauseAnalysis()
                        : incident.getMetricName() + " exceeded threshold"
        );
    }
}