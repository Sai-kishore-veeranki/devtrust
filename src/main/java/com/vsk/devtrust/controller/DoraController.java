package com.vsk.devtrust.controller;

import com.vsk.devtrust.service.DoraMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dora")
@RequiredArgsConstructor
public class DoraController {

    private final DoraMetricsService doraMetricsService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMetrics(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(doraMetricsService.computeMetrics(days));
    }
}