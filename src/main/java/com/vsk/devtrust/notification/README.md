# Notification Module

Sends a Slack message when an incident reaches HIGH or CRITICAL severity.
Drops in as a pure addition — new package only, zero edits to any existing
file, zero required configuration.

---

## Setup

1. In Slack: **Apps → Incoming Webhooks → Add to Slack**, pick a channel,
   copy the webhook URL it gives you.
2. Add it to your `.env`:
   ```
   DEVTRUST_SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
   ```
3. Restart the app. That's it — no other file needs to change.

If you don't set the webhook URL, this module loads, logs one line saying
it's disabled, and does nothing else. It will never block startup or throw
an error, unlike the required secrets (JWT, Postgres) from earlier modules.

---

## Optional config (all have working defaults)

| Env var | Default | What it does |
|---|---|---|
| `DEVTRUST_SLACK_WEBHOOK_URL` | *(unset — disabled)* | Your Slack webhook URL |
| `DEVTRUST_NOTIFY_MIN_SEVERITY` | `HIGH` | Lowest severity that triggers a message (`LOW`/`MEDIUM`/`HIGH`/`CRITICAL`) |
| `DEVTRUST_NOTIFY_POLL_INTERVAL_MS` | `15000` | How often it checks for new incidents |
| `DEVTRUST_NOTIFY_LOOKBACK_MINUTES` | `10` | How far back each check looks |

---

## The one tradeoff worth understanding: polling, not push

The "correct" way to fire a Slack message the instant an incident is
created is one line inside `CorrelationEngine.onAnomaly()`, right next to
where it already broadcasts over WebSocket. That would mean zero delay.

This module doesn't do that — on purpose, because "one new folder, zero
merge conflicts" was the explicit ask. Instead, it polls
`IncidentRepository` (already an existing bean, just reused) every 15
seconds and uses a small new tracking table (`NotificationLogEntity`) to
make sure the same incident never gets Slacked twice.

**What this costs you:** notifications lag actual incident creation by up
to `DEVTRUST_NOTIFY_POLL_INTERVAL_MS`. For a Slack alert, a 0–15 second
delay is very unlikely to matter in practice.

**If you ever want true zero-delay push instead:** it's a two-line change,
not a rewrite. In `CorrelationEngine.java`, right after this existing line:

```java
messagingTemplate.convertAndSend("/topic/incidents", savedEntity);
```

add:

```java
slackNotifier.send(savedEntity);
```

(and inject `SlackNotifier` into `CorrelationEngine`'s constructor like its
other dependencies). At that point this poller becomes a redundant
catch-up safety net rather than the primary path — safe to leave running
or remove either way.

---

## One honesty note

This was built after a sandbox reset, without a live copy of your current
`IncidentEntity` to check field names against. The getters used here
(`getServiceName()`, `getSeverity()`, `getMetricName()`, `getAnomalyValue()`,
`getThreshold()`, `getCommitId()`, `getAuthor()`, `getDeltaSeconds()`,
`getConfidenceScore()`, `getEstimatedRevenueLost()`,
`getEstimatedUsersAffected()`, `isSlaBreached()`) match what was used
consistently earlier in this project's build — but if any of them don't
compile against your actual entity, it's almost certainly a naming
mismatch in `SlackNotifier.java` only, not a structural problem with the
rest of the module.

---

## Files in this folder

```
notification/
├── NotificationLogEntity.java      — idempotency tracking (new table, auto-created)
├── NotificationLogRepository.java
├── SlackNotifier.java              — formats and sends the Slack message
└── IncidentNotificationPoller.java — the scheduled check
```

Drop the whole `notification/` folder into
`src/main/java/com/vsk/devtrust/`.
