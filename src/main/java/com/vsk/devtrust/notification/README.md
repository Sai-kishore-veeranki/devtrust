# Notification Module — Email version

Replaces the Slack version from the earlier delivery. Sends an email when
an incident reaches HIGH or CRITICAL severity.

---

## The one thing that isn't zero-touch: `pom.xml`

Slack needed nothing but an HTTP call, so that version touched no existing
file at all. Email needs Spring's mail support, which isn't on the
classpath by default. Add this one dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

Everything else about this module — no other config file touched, no
required env vars, disabled until configured — is unchanged from the
Slack version's philosophy.

**If you already added `SlackNotifier.java` from the earlier delivery,
delete it** — this folder replaces it with `EmailNotifier.java` and
`MailConfig.java` instead. `NotificationLogEntity`, `NotificationLogRepository`,
and `IncidentNotificationPoller` are all still here, just pointed at the
new notifier.

---

## Setup

**If using Gmail** (simplest for a small team to start with):
1. Turn on 2-Step Verification on the Google account you want to send from
2. Create an [App Password](https://myaccount.google.com/apppasswords) —
   a regular Gmail password won't work for SMTP, Google blocks that
3. Use the app password as `DEVTRUST_SMTP_PASSWORD` below

Add to `.env`:
```
DEVTRUST_SMTP_USERNAME=alerts@yourstartup.com
DEVTRUST_SMTP_PASSWORD=your-app-password
DEVTRUST_NOTIFY_EMAIL_TO=you@yourstartup.com,cofounder@yourstartup.com
```

Restart the app. Any other SMTP provider (SendGrid, AWS SES, your own mail
server) works the same way — just point `DEVTRUST_SMTP_HOST` at it.

---

## Config (all except the three above have working defaults)

| Env var | Default | What it does |
|---|---|---|
| `DEVTRUST_SMTP_HOST` | `smtp.gmail.com` | SMTP server |
| `DEVTRUST_SMTP_PORT` | `587` | SMTP port (587 = STARTTLS) |
| `DEVTRUST_SMTP_USERNAME` | *(unset — disabled)* | SMTP login |
| `DEVTRUST_SMTP_PASSWORD` | *(unset — disabled)* | SMTP password / app password |
| `DEVTRUST_NOTIFY_EMAIL_TO` | *(unset — disabled)* | Comma-separated recipient list |
| `DEVTRUST_NOTIFY_EMAIL_FROM` | *(defaults to username)* | Override the From address |
| `DEVTRUST_NOTIFY_MIN_SEVERITY` | `HIGH` | Lowest severity that triggers an email |
| `DEVTRUST_NOTIFY_POLL_INTERVAL_MS` | `15000` | How often it checks for new incidents |
| `DEVTRUST_NOTIFY_LOOKBACK_MINUTES` | `10` | How far back each check looks |

Notifications stay fully disabled until username, password, AND at least
one recipient are all set.

---

## Same polling tradeoff as the Slack version

This polls `IncidentRepository` every 15 seconds rather than hooking
directly into `CorrelationEngine` — see the Slack README's explanation,
it applies identically here. If you want zero-delay push instead of a
0–15s lag, the fix is the same shape: add
`emailNotifier.send(savedEntity)` right after
`messagingTemplate.convertAndSend("/topic/incidents", savedEntity)` inside
`CorrelationEngine.onAnomaly()`, and inject `EmailNotifier` into its
constructor.

---

## Files in this folder

```
notification/
├── NotificationLogEntity.java      — idempotency tracking (unchanged)
├── NotificationLogRepository.java  — unchanged
├── MailConfig.java                 — builds the JavaMailSender bean
├── EmailNotifier.java              — formats and sends the email
├── IncidentNotificationPoller.java — the scheduled check (updated)
└── README.md
```

Drop the whole `notification/` folder into `src/main/java/com/vsk/devtrust/`,
replacing the existing one if you already added the Slack version.
