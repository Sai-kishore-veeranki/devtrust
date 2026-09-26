package com.vsk.devtrust.notification;

import com.vsk.devtrust.entity.IncidentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotifier implements NotificationProvider {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    @Value("${DEVTRUST_SMTP_USERNAME:}")
    private String smtpUsername;

    @Value("${DEVTRUST_SMTP_PASSWORD:}")
    private String smtpPassword;

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        NotificationProperties.EmailConfig email = properties.getEmail();
        return email.isEnabled()
                && notBlank(smtpUsername)
                && notBlank(smtpPassword)
                && notBlank(email.getDefaultRecipient());
    }

    @Override
    public void sendAlert(IncidentEntity incident) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getEmail().getFrom());
        message.setTo(properties.getEmail().getDefaultRecipient());
        message.setSubject(String.format("[DevTrust Alert][%s] %s - %s",
                incident.getSeverity(), incident.getServiceName(), incident.getMetricName()));
        message.setText(String.format("""
                DevTrust Incident Notification
                =================================
                Incident ID: %s
                Service:     %s
                Severity:    %s
                Status:      %s
                Detected At: %s

                Metric: %s
                Value:  %.2f (threshold %.2f)
                Commit: %s
                Author: %s

                Root Cause:
                %s

                Cost Summary:
                %s
                """,
                incident.getIncidentId(),
                incident.getServiceName(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getDetectedAt() != null
                        ? DateTimeFormatter.ISO_INSTANT.format(incident.getDetectedAt()) : "N/A",
                incident.getMetricName(),
                incident.getAnomalyValue(),
                incident.getThreshold(),
                incident.getCommitId(),
                incident.getAuthor(),
                incident.getRootCauseAnalysis() != null ? incident.getRootCauseAnalysis() : "N/A",
                incident.getCostSummary() != null ? incident.getCostSummary() : "N/A"));
        mailSender.send(message);
        log.info("Email alert dispatched for incident {}", incident.getIncidentId());
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
