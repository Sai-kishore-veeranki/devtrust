package com.vsk.devtrust.notification;

import com.vsk.devtrust.entity.IncidentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final List<NotificationProvider> providers;
    private final NotificationProperties properties;
    private final NotificationLogRepository notificationLogRepository;

    @Transactional
    public void dispatch(IncidentEntity incident) {
        String incidentId = incident.getIncidentId();
        String severity = incident.getSeverity() == null
                ? "DEFAULT" : incident.getSeverity().toUpperCase();
        List<NotificationChannel> channels = properties.getRouting().getOrDefault(
                severity, properties.getRouting().getOrDefault("DEFAULT", List.of(NotificationChannel.EMAIL)));

        Map<NotificationChannel, NotificationProvider> providerMap = providers.stream()
                .collect(Collectors.toMap(NotificationProvider::getChannel,
                        provider -> provider, (first, ignored) -> first, () -> new EnumMap<>(NotificationChannel.class)));

        for (NotificationChannel channel : channels) {
            if (notificationLogRepository.findByIncidentId(incidentId).stream()
                    .anyMatch(logEntry -> channel.name().equals(logEntry.getChannel()))) {
                continue;
            }

            NotificationProvider provider = providerMap.get(channel);
            NotificationLogEntity logEntity = NotificationLogEntity.builder()
                    .incidentId(incidentId)
                    .channel(channel.name())
                    .status("PENDING")
                    .createdAt(Instant.now())
                    .build();
            try {
                if (provider == null || !provider.isEnabled()) {
                    logEntity.setStatus("SKIPPED");
                    log.warn("Channel {} is configured for severity {} but is missing or disabled",
                            channel, severity);
                } else {
                    provider.sendAlert(incident);
                    logEntity.setStatus("SENT");
                    logEntity.setSentAt(Instant.now());
                }
            } catch (Exception ex) {
                log.error("Failed to deliver alert via {} for incident {}", channel, incidentId, ex);
                logEntity.setStatus("FAILED");
                logEntity.setErrorMessage(ex.getMessage());
            }
            notificationLogRepository.save(logEntity);
        }
    }
}
