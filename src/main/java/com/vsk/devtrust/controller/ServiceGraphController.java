package com.vsk.devtrust.controller;

import com.vsk.devtrust.service.ServiceGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class ServiceGraphController {

    private final ServiceGraphService serviceGraphService;

    // Get full graph data for React visualization
    @GetMapping
    public ResponseEntity<Map<String, Object>> getGraph() {
        return ResponseEntity.ok(serviceGraphService.getGraphData());
    }

    // Manually declare a dependency between two services
    @PostMapping("/dependency")
    public ResponseEntity<String> addDependency(@RequestBody Map<String, String> body) {
        String from = body.get("from");
        String to = body.get("to");
        String type = body.getOrDefault("type", "HARD");

        if (from == null || to == null) {
            return ResponseEntity.badRequest().body("from and to are required");
        }

        serviceGraphService.addDependency(from, to, type);
        return ResponseEntity.ok("dependency added");
    }

    // Manually update a service's health status
    @PatchMapping("/nodes/{serviceName}/status")
    public ResponseEntity<String> updateStatus(
            @PathVariable String serviceName,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        serviceGraphService.updateServiceStatus(serviceName, status);
        return ResponseEntity.ok("status updated");
    }
}