package com.vsk.devtrust.warroom;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentWarRoomServiceTest {

    @Test
    void buildSummary_prioritizesMostSevereServiceAndCapturesActions() {
        IncidentWarRoomService service = new IncidentWarRoomService();

        List<IncidentSummaryInput> incidents = List.of(
                new IncidentSummaryInput("inc-1", "payments-service", "CRITICAL", 12450.0, 6200L, 980.0, "database connection leak"),
                new IncidentSummaryInput("inc-2", "gateway-service", "HIGH", 4600.0, 3100L, 640.0, "increased retry bursts"),
                new IncidentSummaryInput("inc-3", "inventory-service", "MEDIUM", 1800.0, 980L, 420.0, "cache warm-up regression")
        );

        IncidentWarRoomSummary summary = service.buildSummary(incidents);

        assertThat(summary.primaryService()).isEqualTo("payments-service");
        assertThat(summary.primarySeverity()).isEqualTo("CRITICAL");
        assertThat(summary.headline()).contains("payments-service");
        assertThat(summary.recommendedActions()).isNotEmpty();
        assertThat(summary.impactedServiceCount()).isEqualTo(3);
        assertThat(summary.totalRevenueLost()).isEqualTo(18850.0);
    }

    @Test
    void buildSummary_returnsSafeEmptyState_forNoIncidents() {
        IncidentWarRoomService service = new IncidentWarRoomService();

        IncidentWarRoomSummary summary = service.buildSummary(List.of());

        assertThat(summary.primaryService()).isEqualTo("No active incident");
        assertThat(summary.headline()).contains("No operational incidents");
        assertThat(summary.recommendedActions()).hasSize(2);
    }
}
