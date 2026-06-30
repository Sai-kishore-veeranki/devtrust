package com.vsk.devtrust.repository;

import com.vsk.devtrust.entity.IncidentEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class IncidentRepositoryTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void existsByCorrelationKey_shouldReturnFalseWhenNoMatch() {
        boolean exists = incidentRepository.existsByCorrelationKey("nonexistent-key");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByCorrelationKey_shouldReturnTrueAfterSaving() {
        IncidentEntity incident = buildIncident("commit123:anomaly456");
        incidentRepository.save(incident);

        boolean exists = incidentRepository.existsByCorrelationKey("commit123:anomaly456");

        assertThat(exists).isTrue();
    }

    @Test
    void savingDuplicateCorrelationKey_shouldThrowDataIntegrityViolationException() {
        // given
        String key = "commit123:anomaly456";
        incidentRepository.saveAndFlush(buildIncident(key));

        // when/then
        IncidentEntity duplicate = buildIncident(key);

        assertThatThrownBy(() -> incidentRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("correlation_key"); // or your constraint name
    }

    private IncidentEntity buildIncident(String correlationKey) {
        return IncidentEntity.builder()
                .incidentId(UUID.randomUUID().toString())
                .correlationKey(correlationKey)
                .serviceName("test-service")
                .commitId("commit123")
                .author("test-author")
                .metricName("test_metric")
                .anomalyValue(100.0)
                .threshold(50.0)
                .severity("HIGH")
                .deltaSeconds(30)
                .confidenceScore(0.8)
                .detectedAt(Instant.now())
                .build();
    }
}