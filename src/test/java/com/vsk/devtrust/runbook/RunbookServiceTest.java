package com.vsk.devtrust.runbook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunbookServiceTest {

    @Mock
    private RunbookRepository repository;

    @Test
    void create_normalizesInputAndGeneratesSlug() {
        RunbookService service = new RunbookService(repository);

        RunbookRequest request = RunbookRequest.builder()
                .title("  Payment Service Latency Recovery  ")
                .serviceName("  payment-service  ")
                .severity(RunbookSeverity.SEV1)
                .trigger("  Checkout failures  ")
                .summary("   ")
                .steps(List.of(RunbookStep.builder()
                        .title("  Confirm incident scope  ")
                        .description("  Review the incident timeline  ")
                        .action("   ")
                        .owner("   ")
                        .expectedOutcome("   ")
                        .build()))
                .owners(new LinkedHashSet<>(List.of(" ops@company.com ", "ops@company.com")))
                .build();

        when(repository.existsBySlug("payment-service-latency-recovery")).thenReturn(false);
        when(repository.save(any(RunbookEntity.class))).thenAnswer(invocation -> {
            RunbookEntity entity = invocation.getArgument(0);
            entity.setId(42L);
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        });

        RunbookEntity saved = service.create(request);

        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getSlug()).isEqualTo("payment-service-latency-recovery");
        assertThat(saved.getTitle()).isEqualTo("Payment Service Latency Recovery");
        assertThat(saved.getServiceName()).isEqualTo("payment-service");
        assertThat(saved.getTrigger()).isEqualTo("Checkout failures");
        assertThat(saved.getSummary()).isEmpty();
        assertThat(saved.getOwners()).containsExactly("ops@company.com");
        assertThat(saved.getSteps()).hasSize(1);
        assertThat(saved.getSteps().get(0).getAction())
                .isEqualTo("Review the service state and continue with the recovery plan.");
        assertThat(saved.getSteps().get(0).getOwner()).isEqualTo("oncall");
    }

    @Test
    void execute_returnsChecklistForEveryStep() {
        RunbookService service = new RunbookService(repository);

        RunbookEntity entity = RunbookEntity.builder()
                .id(7L)
                .slug("auth-service-failure-recovery")
                .title("Auth service failure recovery")
                .serviceName("auth-service")
                .severity(RunbookSeverity.SEV2)
                .trigger("401 spikes")
                .summary("Recover auth traffic")
                .active(true)
                .steps(List.of(
                        RunbookStep.builder().title("Review logs").build(),
                        RunbookStep.builder().title("Scale auth tier").build()))
                .owners(new LinkedHashSet<>(List.of("security@company.com")))
                .build();

        when(repository.findById(7L)).thenReturn(Optional.of(entity));

        RunbookExecutionResult result = service.execute(7L);

        assertThat(result.runbookId()).isEqualTo(7L);
        assertThat(result.title()).isEqualTo("Auth service failure recovery");
        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.executedSteps()).containsExactly("Review logs", "Scale auth tier");
    }

    @Test
    void create_rejectsBlankRequiredFields() {
        RunbookService service = new RunbookService(repository);

        RunbookRequest request = RunbookRequest.builder()
                .title("   ")
                .serviceName("auth-service")
                .severity(RunbookSeverity.SEV1)
                .trigger("Token issue")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title is required.");
    }
}
