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
 * Same design as the Slack version this replaces: polls IncidentRepository
 * on a schedule rather than hooking directly into CorrelationEngine, to
 * keep this module a pure drop-in. See README.md for the exact two-line
 * change if you ever want zero-delay push instead of polling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentNotificationPoller {

    private static final List<String> SEVERITY_ORDER = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private final IncidentRepository incidentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final EmailNotifier emailNotifier;

    @Value("${DEVTRUST_NOTIFY_MIN_SEVERITY:HIGH}")
    private String minSeverity;

    @Value("${DEVTRUST_NOTIFY_LOOKBACK_MINUTES:10}")
    private long lookbackMinutes;

    @Scheduled(fixedDelayString = "${DEVTRUST_NOTIFY_POLL_INTERVAL_MS:15000}")
    public void checkForIncidentsToNotify() {
        if (!emailNotifier.isEnabled()) {
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

            emailNotifier.send(incident);

            notificationLogRepository.save(NotificationLogEntity.builder()
                    .incidentId(incident.getIncidentId())
                    .channel("email")
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
