package com.vsk.devtrust.runbook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunbookRequest {

    @NotBlank(message = "title is required")
    private String title;

    @NotBlank(message = "serviceName is required")
    private String serviceName;

    @NotNull(message = "severity is required")
    @Builder.Default
    private RunbookSeverity severity = RunbookSeverity.SEV2;

    @NotBlank(message = "trigger is required")
    @Builder.Default
    private String trigger = "production incident";

    @Builder.Default
    private String summary = "";

    @Builder.Default
    private List<RunbookStep> steps = new ArrayList<>();

    @Builder.Default
    private Set<String> owners = new LinkedHashSet<>();
}
