package com.vsk.devtrust.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Slow_Test {
    @GetMapping("/api/test/slow-endpoint")
    public ResponseEntity<String> slowEndpoint() throws InterruptedException {
        Thread.sleep(500); // simulates a slow real endpoint
        return ResponseEntity.ok("done");
    }
}
