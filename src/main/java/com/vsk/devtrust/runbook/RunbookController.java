package com.vsk.devtrust.runbook;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/runbooks")
@RequiredArgsConstructor
public class RunbookController {

    private final RunbookService runbookService;

    @GetMapping
    public List<RunbookResponse> listAll() {
        return runbookService.listAll().stream()
                .map(RunbookResponse::fromEntity)
                .toList();
    }

    @GetMapping("/service/{serviceName}")
    public List<RunbookResponse> listByService(@PathVariable String serviceName) {
        return runbookService.listByService(serviceName).stream()
                .map(RunbookResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(RunbookResponse.fromEntity(runbookService.getById(id)));
        } catch (RunbookService.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody RunbookRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(RunbookResponse.fromEntity(runbookService.create(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody RunbookRequest request) {
        try {
            return ResponseEntity.ok(RunbookResponse.fromEntity(runbookService.update(id, request)));
        } catch (RunbookService.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            runbookService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RunbookService.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<?> execute(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(runbookService.execute(id));
        } catch (RunbookService.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
