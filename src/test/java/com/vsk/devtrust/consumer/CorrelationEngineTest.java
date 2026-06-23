package com.vsk.devtrust.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorrelationEngineTest {

    // computeConfidence is private, so we test it via reflection
    // In a real refactor, you'd extract this into a separate, testable ConfidenceCalculator class
    private double invokeComputeConfidence(CorrelationEngine engine, long deltaSeconds, String severity) throws Exception {
        Method method = CorrelationEngine.class.getDeclaredMethod("computeConfidence", long.class, String.class);
        method.setAccessible(true);
        return (double) method.invoke(engine, deltaSeconds, severity);
    }

    @Test
    void criticalSeverityImmediatelyAfterDeploy_givesHighConfidence() throws Exception {
        CorrelationEngine engine = new CorrelationEngine(null, null, null, null, null);
        setWindowSeconds(engine, 300);

        double confidence = invokeComputeConfidence(engine, 5, "CRITICAL");

        assertEquals(0.99, confidence, 0.05);
    }

    @Test
    void lowSeverityNearEndOfWindow_givesLowConfidence() throws Exception {
        CorrelationEngine engine = new CorrelationEngine(null, null, null, null, null);
        setWindowSeconds(engine, 300);

        double confidence = invokeComputeConfidence(engine, 290, "LOW");

        assertEquals(0.13, confidence, 0.05);
    }

    @ParameterizedTest
    @CsvSource({
            "CRITICAL, 1.0",
            "HIGH, 0.8",
            "MEDIUM, 0.5",
            "LOW, 0.3"
    })
    void severityScoreOrdering_isCorrect(String severity, double expectedSeverityWeight) throws Exception {
        CorrelationEngine engine = new CorrelationEngine(null, null, null, null, null);
        setWindowSeconds(engine, 300);

        // At deltaSeconds = 0, timeScore = 1.0, so confidence = (1.0 * 0.6) + (severityWeight * 0.4)
        double confidence = invokeComputeConfidence(engine, 0, severity);
        double expected = (1.0 * 0.6) + (expectedSeverityWeight * 0.4);

        assertEquals(expected, confidence, 0.01);
    }

    private void setWindowSeconds(CorrelationEngine engine, long seconds) throws Exception {
        var field = CorrelationEngine.class.getDeclaredField("correlationWindowSeconds");
        field.setAccessible(true);
        field.set(engine, seconds);
    }
}