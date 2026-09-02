Async AI worker design

Goal: make Groq API calls asynchronous and resilient by moving them off the Kafka consumer thread.

Approach:
- Producer: CorrelationEngine publishes a lightweight AI request to topic `devtrust.ai-requests` containing incidentId and context.
- Consumer (worker): separate Spring Boot @KafkaListener in a small consumer group `devtrust-ai-workers` picks up requests, calls Groq API, persists AI result to `incident` record (ai_explanation column) and emits an incident-updated event for the frontend.
- Resilience: add retry with exponential backoff, circuit breaker (Resilience4j) and persistent dead-letter topic `devtrust.ai-dlq` for failed requests.
- Observability: record latency metrics (micrometer) and add tracing spans.

Minimal code sketch:

```java
@Component
@KafkaListener(topics = "devtrust.ai-requests", groupId = "devtrust-ai-workers")
public class AsyncAiWorker {
    @Autowired private RestTemplate restTemplate;
    @Autowired private IncidentRepository incidentRepository;

    @KafkaHandler
    public void handle(AiRequest req) {
        try {
            // call Groq API, update incident record
        } catch (Exception e) {
            // retry/backoff or send to DLQ
        }
    }
}
```

Migration: add `ai_explanation TEXT` column to `incident` table in the next Flyway migration.

Operations: deploy worker with scaled replicas based on throughput; set GROQ_API_KEY as secret in deployment environment.
