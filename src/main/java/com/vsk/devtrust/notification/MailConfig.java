package com.vsk.devtrust.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Spring Boot's own mail autoconfiguration only activates when
 * spring.mail.host is set as a real property, which would mean touching
 * application.yaml. Building the bean directly here from plain env vars
 * (with inline defaults) keeps this module additive-only, consistent with
 * the auth and Slack modules before it.
 *
 * This bean is created even with blank credentials — it just won't be
 * usable until EmailNotifier's isEnabled() check passes. Nothing here
 * fails startup on its own.
 */
@Configuration
public class MailConfig {

    @Value("${DEVTRUST_SMTP_HOST:smtp.gmail.com}")
    private String host;

    @Value("${DEVTRUST_SMTP_PORT:587}")
    private int port;

    @Value("${DEVTRUST_SMTP_USERNAME:}")
    private String username;

    @Value("${DEVTRUST_SMTP_PASSWORD:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return sender;
    }
}
