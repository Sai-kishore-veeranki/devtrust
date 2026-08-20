package com.vsk.devtrust.ai;

import com.vsk.devtrust.entity.IncidentEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RootCauseAnalysisServiceTest {

    @Mock
    private RestTemplate restTemplate;

    /**
     * Groq's real response shape includes a "usage" block with small-float
     * timing fields (queue_time, prompt_time, completion_time, total_time) as
     * siblings of "choices". This pins that the service extracts
     * choices[0].message.content and never accidentally surfaces one of those
     * timing floats as if it were the analysis text.
     */
    @Test
    void generateRootCause_returnsMessageContent_notAUsageTimingField() {
        RootCauseAnalysisService service = newService();

        String groqStyleResponse = """
                {
                  "choices": [
                    {
                      "message": { "role": "assistant", "content": "Latency spiked after commit abc6011 introduced a synchronous call in the hot path." },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "queue_time": 0.0011221840977668762,
                    "prompt_time": 0.0089,
                    "completion_time": 0.0654,
                    "total_time": 0.0743
                  }
                }
                """;

        when(restTemplate.postForObject(any(String.class), any(), eq(String.class)))
                .thenReturn(groqStyleResponse);

        String result = service.generateRootCause(sampleIncident());

        assertThat(result).contains("Latency spiked");
        assertThat(result).doesNotContain("0.001");
    }

    @Test
    void generateRootCause_fallsBackCleanly_whenContentIsNotText() {
        RootCauseAnalysisService service = newService();

        // Content missing entirely, e.g. finish_reason: "length" with an empty body
        String malformedResponse = """
                {
                  "choices": [
                    { "message": { "role": "assistant" }, "finish_reason": "length" }
                  ],
                  "usage": { "queue_time": 0.0011 }
                }
                """;

        when(restTemplate.postForObject(any(String.class), any(), eq(String.class)))
                .thenReturn(malformedResponse);

        String result = service.generateRootCause(sampleIncident());

        assertThat(result).isEqualTo("AI analysis unavailable. Manual investigation required.");
    }

    /**
     * Groq's API reference explicitly lists "max_tokens" as deprecated in favor
     * of "max_completion_tokens" — the opposite of what an earlier pass through
     * this file assumed. Pinning the correct param name here so that mistake
     * can't quietly get reintroduced.
     */
    @Test
    @SuppressWarnings("unchecked")
    void generateRootCause_sendsMaxCompletionTokens_notDeprecatedMaxTokens() {
        RootCauseAnalysisService service = newService();

        when(restTemplate.postForObject(any(String.class), any(), eq(String.class)))
                .thenReturn("""
                        { "choices": [ { "message": { "content": "ok" }, "finish_reason": "stop" } ] }
                        """);

        service.generateRootCause(sampleIncident());

        ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(any(String.class), captor.capture(), eq(String.class));

        Map<String, Object> body = captor.getValue().getBody();
        assertThat(body).containsKey("max_completion_tokens");
        assertThat(body).doesNotContainKey("max_tokens");
    }

    private RootCauseAnalysisService newService() {
        RootCauseAnalysisService service = new RootCauseAnalysisService(restTemplate);
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.groq.com/openai/v1/chat/completions");
        ReflectionTestUtils.setField(service, "model", "llama-3.3-70b-versatile");
        return service;
    }

    private IncidentEntity sampleIncident() {
        IncidentEntity incident = new IncidentEntity();
        incident.setIncidentId("inc-test-1");
        incident.setServiceName("notification-service");
        incident.setCommitId("abc6011");
        incident.setAuthor("diana");
        incident.setMetricName("p99_latency_ms");
        incident.setAnomalyValue(149.0);
        incident.setThreshold(75.0);
        incident.setSeverity("HIGH");
        incident.setDeltaSeconds(60);
        incident.setConfidenceScore(0.80);
        return incident;
    }
}