# Notification module

New incidents are picked up by `IncidentNotificationPoller` and routed by
severity through the configured `NotificationProvider` implementations:

- `EMAIL` sends SMTP mail.
- `SLACK` sends a Slack Block Kit webhook message.
- `PAGERDUTY` triggers Events API v2 with a stable deduplication key.

Each attempted channel delivery is stored in `notification_log` with
`PENDING`, `SENT`, `FAILED`, or `SKIPPED` status. The incident/channel unique
constraint prevents duplicate delivery when a poll overlaps.

All channels are disabled by default. Enable only the channels that have real
credentials configured:

```text
DEVTRUST_NOTIFICATIONS_EMAIL_ENABLED=true
DEVTRUST_SMTP_USERNAME=alerts@example.com
DEVTRUST_SMTP_PASSWORD=<smtp-password>
DEVTRUST_NOTIFY_EMAIL_TO=oncall@example.com

DEVTRUST_NOTIFICATIONS_SLACK_ENABLED=true
SLACK_WEBHOOK_URL=<slack-webhook>

DEVTRUST_NOTIFICATIONS_PAGERDUTY_ENABLED=true
PAGERDUTY_ROUTING_KEY=<pagerduty-integration-key>
```

Routing is configured under `devtrust.notifications.routing` in
`application.yaml`. The poller lookback and interval can be adjusted with
`DEVTRUST_NOTIFY_LOOKBACK_MINUTES` and `DEVTRUST_NOTIFY_POLL_INTERVAL_MS`.
