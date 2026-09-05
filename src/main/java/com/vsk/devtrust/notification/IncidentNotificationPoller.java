package com.vsk.devtrust.notification;

import com.vsk.devtrust.entity.IncidentEntity;
import com.vsk.devtrust.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * DESIGN NOTE — polling vs. a direct push:
 *
 * The "correct" way to fire a notification the instant an incident is
 * created is a one-line addition inside CorrelationEngine.onAnomaly(), right
 * next to where it already does messagingTemplate.convertAndSend(...). That
 * would mean zero polling delay.
 *
 * This module deliberately does NOT do that, because it was asked to be a
 * pure drop-in with no edits to existing files. Instead it polls
 * IncidentRepository on a schedule and uses NotificationLogEntity to avoid
 * double-sending. The real tradeoff: notifications lag incident creation by
 * up to DEVTRUST_NOTIFY_POLL_INTERVAL_MS (default 15s), and every poll
 * re-scans a rolling lookback window rather than reacting to a single event.
 * Fine for a Slack alert; if that latency ever matters, the fix is trivial —
 * add one line to CorrelationEngine calling slackNotifier.send(savedEntity)
 * right after it's saved, and this poller becomes a backup/catch-up path
 * instead of the primary one.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentNotificationPoller {

    private static final List<String> SEVERITY_ORDER = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final IncidentRepository incidentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SlackNotifier slackNotifier;

    @Value("${DEVTRUST_NOTIFY_MIN_SEVERITY:HIGH}")
    private String minSeverity;

    @Value("${DEVTRUST_NOTIFY_LOOKBACK_MINUTES:10}")
    private long lookbackMinutes;

    @Scheduled(fixedDelayString = "${DEVTRUST_NOTIFY_POLL_INTERVAL_MS:15000}")
    public void checkForIncidentsToNotify() {
        if (!slackNotifier.isEnabled()) {
            return;
        }

        Instant since = Instant.now().minus(lookbackMinutes, ChronoUnit.MINUTES);
        List<IncidentEntity> recent = incidentRepository.findByDetectedAtAfterOrderByDetectedAtDesc(since);

        for (IncidentEntity incident : recent) {
            if (!meetsThreshold(incident.getSeverity())) {
                continue;
            }
            if (notificationLogRepository.existsByIncidentId(incident.getIncidentId())) {
                continue;
            }

            slackNotifier.send(incident);

            notificationLogRepository.save(NotificationLogEntity.builder()
                    .incidentId(incident.getIncidentId())
                    .channel("slack")
                    .notifiedAt(Instant.now())
                    .build());
        }
    }

    private boolean meetsThreshold(String severity) {
        int incidentRank = SEVERITY_ORDER.indexOf(severity);
        int thresholdRank = SEVERITY_ORDER.indexOf(minSeverity);
        return incidentRank >= 0 && incidentRank >= thresholdRank;
    }
}
