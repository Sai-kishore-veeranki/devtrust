package com.vsk.devtrust.notification;

import com.vsk.devtrust.entity.IncidentEntity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotifier {

    private final JavaMailSender mailSender;

    @Value("${DEVTRUST_SMTP_USERNAME:}")
    private String smtpUsername;

    @Value("${DEVTRUST_SMTP_PASSWORD:}")
    private String smtpPassword;

    // Comma-separated for multiple recipients, e.g. "alice@x.com,bob@x.com"
    @Value("${DEVTRUST_NOTIFY_EMAIL_TO:}")
    private String toAddresses;

    // Optional — defaults to the SMTP username if not set separately
    @Value("${DEVTRUST_NOTIFY_EMAIL_FROM:}")
    private String fromAddressOverride;

    @PostConstruct
    void logStatus() {
        if (isEnabled()) {
            log.info("Email notifications enabled — sending to {}", toAddresses);
        } else {
            log.info("Email notifications disabled — set DEVTRUST_SMTP_USERNAME, " +
                    "DEVTRUST_SMTP_PASSWORD, and DEVTRUST_NOTIFY_EMAIL_TO to enable.");
        }
    }

    public boolean isEnabled() {
        return notBlank(smtpUsername) && notBlank(smtpPassword) && notBlank(toAddresses);
    }

    public void send(IncidentEntity incident) {
        if (!isEnabled()) {
            return;
        }

        String fromAddress = notBlank(fromAddressOverride) ? fromAddressOverride : smtpUsername;
        String[] recipients = toAddresses.split(",");
        for (int i = 0; i < recipients.length; i++) {
            recipients[i] = recipients[i].trim();
        }

        String subject = String.format("[DevTrust] %s incident — %s",
                incident.getSeverity(), incident.getServiceName());

        String body = String.format(
                "%s severity incident detected on %s%n%n" +
                "Metric: %s = %.1f (threshold %.1f)%n" +
                "Deploy: %s by %s — %ds before the anomaly (%.0f%% confidence)%n" +
                "Impact: $%.2f at risk, ~%d users affected%s%n%n" +
                "%s",
                incident.getSeverity(),
                incident.getServiceName(),
                incident.getMetricName(),
                incident.getAnomalyValue(),
                incident.getThreshold(),
                incident.getCommitId(),
                incident.getAuthor(),
                incident.getDeltaSeconds(),
                incident.getConfidenceScore() * 100,
                incident.getEstimatedRevenueLost(),
                incident.getEstimatedUsersAffected(),
                incident.isSlaBreached() ? "\n*** SLA BREACHED ***" : "",
                incident.getCostSummary() != null ? incident.getCostSummary() : ""
        );

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipients);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // A failed send should never affect incident detection itself —
            // log and move on, same principle as the Slack version.
            log.error("Failed to send email notification for incident [{}]", incident.getIncidentId(), e);
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
