package com.vsk.devtrust.runbook;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "runbooks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunbookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RunbookSeverity severity = RunbookSeverity.SEV2;

    @Column(nullable = false)
    private String trigger;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "runbook_steps", joinColumns = @JoinColumn(name = "runbook_id"))
    @OrderColumn(name = "step_order")
    @Builder.Default
    private List<RunbookStep> steps = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "runbook_owners", joinColumns = @JoinColumn(name = "runbook_id"))
    @Column(name = "owner_email")
    @Builder.Default
    private Set<String> owners = new LinkedHashSet<>();
}
