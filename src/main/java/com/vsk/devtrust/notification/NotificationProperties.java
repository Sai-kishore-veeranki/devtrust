package com.vsk.devtrust.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "devtrust.notifications")
public class NotificationProperties {

    private EmailConfig email = new EmailConfig();
    private SlackConfig slack = new SlackConfig();
    private PagerDutyConfig pagerduty = new PagerDutyConfig();
    private Map<String, List<NotificationChannel>> routing = new HashMap<>();

    @Getter
    @Setter
    public static class EmailConfig {
        private boolean enabled;
        private String from = "alerts@devtrust.io";
        private String defaultRecipient;
    }

    @Getter
    @Setter
    public static class SlackConfig {
        private boolean enabled;
        private String webhookUrl;
        private String channelName = "#incident-alerts";
    }

    @Getter
    @Setter
    public static class PagerDutyConfig {
        private boolean enabled;
        private String routingKey;
        private String eventsApiUrl = "https://events.pagerduty.com/v2/enqueue";
    }
}
