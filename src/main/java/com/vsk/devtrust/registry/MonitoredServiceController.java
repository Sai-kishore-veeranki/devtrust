package com.vsk.devtrust.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Every endpoint here is under /api/**, so if the auth module (SecurityConfig)
 * is already in the project, these are protected automatically by its
 * `.anyRequest().authenticated()` catch-all — nothing needed here or there to
 * wire that up. If the auth module ISN'T present yet, these endpoints are
 * open, same as every other /api/** endpoint currently is.
 */
@RestController
@RequestMapping("/api/registry/services")
@RequiredArgsConstructor
public class MonitoredServiceController {

    private final MonitoredServiceService service;

    @GetMapping
    public List<MonitoredService> listAll() {
        return service.listAll();
    }

    @GetMapping("/{serviceName}")
    public ResponseEntity<?> get(@PathVariable String serviceName) {
        try {
            return ResponseEntity.ok(service.get(serviceName));
        } catch (MonitoredServiceService.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MonitoredService input) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(input));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{serviceName}")
    public ResponseEntity<?> update(@PathVariable String serviceName, @RequestBody MonitoredService updates) {
        try {
            return ResponseEntity.ok(service.update(serviceName, updates));
        } catch (MonitoredServiceService.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{serviceName}")
    public ResponseEntity<?> delete(@PathVariable String serviceName) {
        try {
            service.delete(serviceName);
            return ResponseEntity.noContent().build();
        } catch (MonitoredServiceService.NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
