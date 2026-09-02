package com.vsk.devtrust.integration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicIT {

    @Test
    void postgresStarts() {
        try (PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")) {
            pg.start();
            assertTrue(pg.isRunning());
        }
    }
}
