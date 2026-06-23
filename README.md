# DevTrust

A real-time engineering observability platform that automatically correlates production anomalies with the deployments that likely caused them, then uses AI to generate a plain-language root cause analysis.

## The problem

When production breaks, engineers typically spend 20-40 minutes manually checking recent deploys, asking in Slack who shipped what, and scrolling through dashboards to find the cause. DevTrust automates that correlation and writes the explanation for you.

## Architecture

GitHub push → Spring Boot webhook (HMAC-verified) → Kafka → Redis (5-min correlation window)
↓
React dashboard ← WebSocket ← PostgreSQL ← Groq AI root cause ← Correlation match ← Prometheus anomaly

## Tech stack

- **Backend**: Spring Boot 4, Kafka, Redis, PostgreSQL, WebSocket (STOMP)
- **Monitoring**: Prometheus + Micrometer (real JVM/HTTP metrics, no simulated data)
- **AI**: Groq API (Llama/GPT-OSS) for automated root cause generation
- **Frontend**: React, STOMP over SockJS for live updates
- **Security**: HMAC-SHA256 webhook signature verification with constant-time comparison

## How it works

1. A real `git push` triggers a signed GitHub webhook
2. Spring Boot verifies the signature, publishes a `DeploymentEvent` to Kafka
3. The event is cached in Redis with a 300-second TTL (the correlation window)
4. Prometheus continuously scrapes real HTTP latency and JVM heap metrics
5. When a metric crosses a threshold, an `AnomalyEvent` is published to Kafka
6. The correlation engine checks Redis for a recent deployment to the same service
7. If found, a confidence score is computed from time proximity + severity
8. The incident is saved to PostgreSQL and broadcast live over WebSocket
9. Groq generates a plain-language root cause analysis, which updates the incident in place

## Running locally

\`\`\`bash
docker-compose up -d
export GROQ_API_KEY=your_key_here
export GITHUB_WEBHOOK_SECRET=your_secret_here
./mvnw spring-boot:run
\`\`\`

In a separate terminal:
\`\`\`bash
cd devtrust-frontend
npm install
npm run dev
\`\`\`

## What I'd improve with more time

- Move AI calls to an async queue instead of blocking the Kafka consumer thread
- Replace reflection-based testing with an extracted, directly-testable `ConfidenceScorer` class
- Add idempotent message handling to prevent duplicate incidents on consumer retry
- Real Datadog/New Relic integration alongside Prometheus for production parity