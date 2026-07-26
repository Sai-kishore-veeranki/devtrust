# DevTrust

> Real-time engineering intelligence platform that automatically correlates production anomalies with deployments, quantifies business impact, and generates AI-powered root cause analysis — before your CEO asks.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-green?style=flat-square)
![React](https://img.shields.io/badge/React-18-blue?style=flat-square)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.x-black?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-blue?style=flat-square)

---

## The Problem

When production breaks, engineering teams spend 20–40 minutes doing this manually:

- Scrolling GitHub to find who deployed what and when
- Cross-referencing Datadog or Grafana to find which metric spiked
- Asking in Slack "did anyone deploy payment-service recently?"
- Writing a post-mortem from scratch after the fact

**DevTrust automates the entire chain** — from the moment a commit is pushed to the moment a plain-English explanation of what broke and what it cost appears on your dashboard.

---

## How It Works

```
GitHub Push → Webhook (HMAC-verified) → Kafka → Redis (correlation window)
                                                          ↓
React Dashboard ← WebSocket ← PostgreSQL ← Groq AI ← Correlation Match ← Prometheus Anomaly
```

1. A real `git push` triggers a **HMAC-SHA256 signed GitHub webhook**
2. Spring Boot verifies the signature and publishes a `DeploymentEvent` to Kafka
3. The event is cached in **Redis** with a configurable TTL (correlation window)
4. **Prometheus + Micrometer** continuously scrapes real HTTP latency and JVM heap metrics
5. When a metric crosses a threshold, an `AnomalyEvent` is published to Kafka
6. The **correlation engine** checks Redis for a recent deployment to the same service
7. A **confidence score** is computed from time proximity and severity
8. **Blast radius** is calculated: revenue lost, users affected, SLA breach risk
9. **Groq AI** generates a plain-language root cause analysis
10. The incident is broadcast live via **WebSocket** to the React dashboard
11. **Pre-deploy risk scoring** runs on every new PR — a bot comment appears on GitHub before code ships

---

## Features

### Deployment Causality Engine
Automatically links every production anomaly to the exact commit, PR, and engineer that caused it. No manual correlation. No Slack asking.

### AI Root Cause Analysis
When a correlation is detected, Groq's LLM generates a structured, plain-English explanation from the commit data and metric values — drafted in seconds, not hours.

### Blast Radius Costing
Every incident is automatically costed in real dollars: revenue lost per minute, users potentially affected, and SLA breach risk — computed from configurable per-service business metrics.

### Pre-Deploy Risk Scorer
When a PR is opened, DevTrust posts a risk score (0–100) as a bot comment directly on the GitHub PR — before the code ships. Scored from incident history, changed file count, critical path analysis, and PR title keywords.

### DORA Metrics Hub
Live engineering health dashboard: deployment frequency, change failure rate, and mean time to recovery — computed from real incident and deployment data, filterable by time period.

### Service Dependency Graph
Interactive D3.js visualization of every service, its health status (healthy/degraded/critical), and its dependencies. Click any node to see incident history and blast radius. Hard dependencies shown as solid lines, soft as dashed.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.1, Java 21 |
| Event streaming | Apache Kafka 3.x |
| Caching / correlation | Redis 7.2 |
| Database | PostgreSQL 16 |
| Real-time transport | WebSocket (STOMP over SockJS) |
| Monitoring | Prometheus + Micrometer |
| AI | Groq API (openai/gpt-oss-120b) |
| Frontend | React 18, Vite, D3.js |
| Security | HMAC-SHA256 webhook signature verification |
| Containerization | Docker, Docker Compose |

---

## Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose
- Node.js 18+
- A GitHub account with a repository to monitor
- A [Groq API key](https://console.groq.com) (free tier)
- A GitHub Personal Access Token with `repo` scope

### 1. Clone the repository

```bash
git clone https://github.com/Sai-kishore-veeranki/devtrust.git
cd devtrust
```

### 2. Configure environment variables

Create a `.env` file in the project root:

```env
GROQ_API_KEY=your_groq_api_key_here
GITHUB_WEBHOOK_SECRET=your_webhook_secret_here
GITHUB_TOKEN=your_github_personal_access_token_here
```

### 3. Start the full stack

```bash
docker-compose up --build
```

This starts Kafka, Zookeeper, Redis, PostgreSQL, Prometheus, and the Spring Boot app. Wait ~60 seconds for all services to initialize.

### 4. Start the frontend

```bash
cd devtrust-frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

### 5. Connect your GitHub repository

Go to your GitHub repository → **Settings → Webhooks → Add webhook**:

| Field | Value |
|---|---|
| Payload URL | `http://your-server:8080/webhooks/github` |
| Content type | `application/json` |
| Secret | Your `GITHUB_WEBHOOK_SECRET` value |
| Events | Pushes + Pull requests |

### 6. Trigger your first correlation

```bash
# Push a commit to your monitored repository
echo "test" >> README.md && git add . && git commit -m "test: trigger devtrust" && git push

# Generate load to push latency above threshold
for i in {1..20}; do curl -s http://localhost:8080/api/test/slow-endpoint > /dev/null; done
```

Within 15–30 seconds, an incident card will appear on your dashboard with a cost estimate and AI explanation.

---

## API Reference

### Incidents

```
GET   /api/incidents                          Latest 20 incidents
GET   /api/incidents/{id}                     Single incident by ID
GET   /api/incidents/service/{serviceName}    Incidents for a specific service
PATCH /api/incidents/{id}/resolve             Mark incident as resolved
```

### DORA Metrics

```
GET   /api/dora?days=30                       Engineering health metrics for last N days
```

### Service Graph

```
GET   /api/graph                              Full dependency graph (nodes + edges)
POST  /api/graph/dependency                   Add a service dependency
PATCH /api/graph/nodes/{serviceName}/status   Manually update service health status
```

### Webhooks

```
POST  /webhooks/github                        GitHub webhook receiver (push + pull_request)
```

---

## Configuration

Key configuration values in `application.yml`:

```yaml
devtrust:
  correlation:
    window-seconds: 900        # How long after a deploy to correlate anomalies
  prometheus:
    url: http://localhost:9090
    service-name: your-service  # Must match your GitHub repo name exactly
  ai:
    model: openai/gpt-oss-120b
  github:
    webhook-secret: ${GITHUB_WEBHOOK_SECRET}
    token: ${GITHUB_TOKEN}
```

---

## Architecture Deep Dive

### Correlation Engine

The core of DevTrust is the correlation engine inside `CorrelationEngine.java`. It runs two parallel Kafka consumers:

- `onDeployment()` — receives `DeploymentEvent` and writes to Redis with a TTL equal to `correlation.window-seconds`
- `onAnomaly()` — receives `AnomalyEvent`, looks up Redis for a recent deployment to the same service, and if found within the window, creates a `CorrelatedIncident`

**Idempotency:** A deterministic `correlationKey` (`commitId:anomalyId`) with a database unique constraint prevents duplicate incidents on Kafka message redelivery. A `DataIntegrityViolationException` catch handles the race condition case.

**Resilience:** Redis operations are individually try-caught so a Redis outage degrades correlation without crashing the Kafka consumer thread.

### Confidence Scoring

Each incident is assigned a confidence score (0.0–1.0):

```
confidence = (timeScore × 0.6) + (severityScore × 0.4)

timeScore     = 1.0 - (deltaSeconds / windowSeconds)
severityScore = CRITICAL:1.0 | HIGH:0.8 | MEDIUM:0.5 | LOW:0.3
```

### Blast Radius

Per-service business metrics (revenue per minute, active users per minute, SLA threshold) are stored in PostgreSQL and configurable via the `service_business_config` table. On every correlation, blast radius is computed synchronously before the AI call so cost data appears immediately on the dashboard.

### Pre-Deploy Risk Scoring

Four signals contribute to the 0–100 risk score:

| Signal | Weight |
|---|---|
| Recent incident history for this service | 0–40 pts |
| Number of changed files | 0–30 pts |
| Critical path files touched (config, auth, payment) | 0–20 pts |
| PR title keywords (refactor, migrate, breaking) | 0–10 pts |

---

## Project Structure

```
devtrust/
├── src/main/java/com/vsk/devtrust/
│   ├── ai/                    # Groq AI root cause analysis
│   ├── config/                # Kafka, Redis, WebSocket, CORS, data seeding
│   ├── consumer/              # Kafka consumers (CorrelationEngine)
│   ├── controller/            # REST controllers + GitHub webhook
│   ├── detector/              # Prometheus anomaly detection
│   ├── entity/                # JPA entities
│   ├── model/                 # Event models (DeploymentEvent, AnomalyEvent, etc.)
│   ├── repository/            # Spring Data JPA repositories
│   ├── service/               # Business logic (blast radius, risk scorer, graph, DORA)
│   └── simulator/             # Event simulator (disabled in production)
├── devtrust-frontend/
│   └── src/
│       ├── components/        # React components
│       │   ├── IncidentCard.jsx
│       │   ├── IncidentFeed.jsx
│       │   ├── DoraMetrics.jsx
│       │   └── ServiceGraph.jsx
│       └── services/          # API and WebSocket clients
├── prometheus.yml             # Prometheus scrape config
├── docker-compose.yml         # Full stack local + production setup
├── Dockerfile                 # Multi-stage Spring Boot build
└── README.md
```

---

## Known Limitations and Future Work

**Change failure rate accuracy:** The current DORA implementation only tracks deployments that caused incidents. A complete implementation would require a separate deployments table tracking all pushes regardless of outcome.

**AI call blocking:** The Groq API call currently blocks the Kafka consumer thread (typically 1–3 seconds). At scale, this should move to an async queue with a separate consumer group.

**Service dependency auto-discovery:** Dependencies are currently declared manually via the REST API or seeded on startup. A production-grade implementation would auto-discover them from service mesh telemetry (Istio, Consul) or distributed tracing (Jaeger, Zipkin).

**Single-tenant:** The current data model has no tenant isolation. Multi-company use would require a `tenantId` threaded through all entities and Kafka message keys.

---

## Lessons Learned

**String matching is the silent killer in distributed systems.** Three separate bugs across this project traced to a single stray character in a service name string. When services correlate by key matching, even one character difference means a silent miss with no error thrown anywhere in the system.

**Prometheus PromQL with per-label cardinality needs aggregation.** A naive `rate(metric[1m])` returns one series per unique label combination. Without `sum()`, dividing mismatched series produces garbage values including negative billions for heap usage. The correct pattern is always `sum(rate(...)) / sum(rate(...))` for application-wide averages.

**Application-level deduplication is not sufficient alone.** The `existsByCorrelationKey()` check reduces duplicate incidents but does not eliminate them — two threads can both pass the check before either saves. The database unique constraint is the only truly atomic safety net, and the `DataIntegrityViolationException` catch is the correct pattern for handling the race condition gracefully.

**TTL-based correlation windows require discipline in testing.** A 300-second Redis TTL feels generous but is easy to exceed during manual testing. Either widen the window for development or build a test harness that generates both signals immediately after each other.

---

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

---

## License

MIT — see [LICENSE](LICENSE) for details.

---

## Author

**Sai Kishore Veeranki**  
[GitHub](https://github.com/Sai-kishore-veeranki)