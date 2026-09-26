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

@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentNotificationPoller {

    private final IncidentRepository incidentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Value("${DEVTRUST_NOTIFY_LOOKBACK_MINUTES:10}")
    private long lookbackMinutes;

    @Scheduled(fixedDelayString = "${DEVTRUST_NOTIFY_POLL_INTERVAL_MS:10000}")
    public void pollAndNotifyNewIncidents() {
        Instant since = Instant.now().minus(lookbackMinutes, ChronoUnit.MINUTES);
        List<IncidentEntity> incidents = incidentRepository
                .findByDetectedAtAfterOrderByDetectedAtDesc(since);

        for (IncidentEntity incident : incidents) {
            if (notificationLogRepository.findByIncidentId(incident.getIncidentId()).isEmpty()) {
                notificationDispatcher.dispatch(incident);
            }
        }
    }
}
