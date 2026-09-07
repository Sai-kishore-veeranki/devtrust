# Service Registry Module

A database-backed place to configure per-service thresholds and metadata,
replacing static YAML/hardcoded Java values — the foundation the
"deployment ingestion" and "metrics ingestion" integration-friendliness
modules both depend on.

Drops in as a pure addition: new package only, zero edits to any existing
file, zero required configuration. New table auto-creates via the existing
`ddl-auto: update` setting.

---

## What this module does today

A working CRUD API for service configuration:

```
GET    /api/registry/services              — list all
GET    /api/registry/services/{name}       — get one
POST   /api/registry/services              — create
PUT    /api/registry/services/{name}       — update
DELETE /api/registry/services/{name}       — delete
```

Example create request:
```json
{
  "serviceName": "checkout-service",
  "repoUrl": "https://github.com/yourorg/checkout-service",
  "latencyThresholdMs": 250.0,
  "heapThresholdPercent": 80.0,
  "businessTier": "TIER_1"
}
```

If the auth module is already in the project, these endpoints are
automatically protected by `SecurityConfig`'s existing
`.anyRequest().authenticated()` rule — no changes needed there either.

---

## What this module does NOT do yet (important)

**It's a config store, not a live integration point.**
`PrometheusAnomalyDetector` and `GitHubWebhookController` still read their
thresholds and secrets from static config (`application.yaml`, env vars),
not from rows in this new table. Adding a service here doesn't yet change
detection behavior for that service.

Wiring those two together is the natural next step, but it means editing
two existing files:

- `PrometheusAnomalyDetector` would loop over
  `monitoredServiceRepository.findByActiveTrue()` instead of using one
  hardcoded service name and threshold pair
- `GitHubWebhookController` would look up the per-service `webhookSecret`
  from this table instead of the single global
  `GITHUB_WEBHOOK_SECRET`

Kept out of this delivery on purpose, matching the "zero merge conflict"
pattern from the last two modules — say the word when you're ready for
that pass, understanding it touches existing code this time.

**Prometheus limitation, specifically:** even after that wiring exists,
adding a `prometheusTarget` value here doesn't make Prometheus itself
start scraping it — Prometheus's own `prometheus.yml` is still a static
file. Making scrape targets fully dynamic needs either Prometheus's file-based
service discovery (writing target files this module generates) or its HTTP
service discovery API — a bigger, separate piece of work if you want it.

---

## Files in this folder

```
registry/
├── MonitoredService.java            — entity (new table, auto-created)
├── MonitoredServiceRepository.java
├── MonitoredServiceService.java     — validation + CRUD logic
├── MonitoredServiceController.java  — REST API
└── README.md
```

Drop the whole `registry/` folder into `src/main/java/com/vsk/devtrust/`.
