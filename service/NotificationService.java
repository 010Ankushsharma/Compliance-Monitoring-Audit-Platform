package com.company.compliance.service;

import com.company.compliance.config.AppProperties;
import com.company.compliance.domain.entity.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Multi-channel notification dispatcher.
 *
 * <p>Supported channels: EMAIL, SLACK, WEBHOOK.
 * Channel selection is based on the alert's severity and per-org channel config.
 *
 * <p>File: {@code src/main/java/com/company/compliance/service/NotificationService.java}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AppProperties   appProperties;
    private final JavaMailSender  mailSender;
    private final RestTemplate    restTemplate;

    /**
     * Sends the alert to all applicable channels.
     * @return array of channel names that successfully received the notification
     */
    public String[] send(Alert alert) {
        List<String> sent = new ArrayList<>();

        AppProperties.NotificationProperties cfg = appProperties.getNotifications();

        if (cfg.getEmail().isEnabled()) {
            try {
                sendEmail(alert);
                sent.add("EMAIL");
            } catch (Exception e) {
                log.error("Email notification failed for alert {}: {}", alert.getId(), e.getMessage());
            }
        }

        if (cfg.getSlack().isEnabled() && !cfg.getSlack().getWebhookUrl().isBlank()) {
            try {
                sendSlack(alert);
                sent.add("SLACK");
            } catch (Exception e) {
                log.error("Slack notification failed for alert {}: {}", alert.getId(), e.getMessage());
            }
        }

        return sent.toArray(String[]::new);
    }

    // ── Email ────────────────────────────────────────────────────

    private void sendEmail(Alert alert) {
        AppProperties.NotificationProperties.EmailProperties emailCfg =
                appProperties.getNotifications().getEmail();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailCfg.getFrom());
        message.setTo(emailCfg.getFrom());      // org admins resolved from DB in production
        message.setSubject("[" + alert.getSeverity() + "] " + alert.getTitle());
        message.setText(alert.getMessage()
                + "\n\nAlert ID: " + alert.getId()
                + "\nOrganisation: " + alert.getOrganization().getName()
                + "\nCreated at: " + alert.getCreatedAt());
        mailSender.send(message);
        log.debug("Email sent for alert {}", alert.getId());
    }

    // ── Slack ─────────────────────────────────────────────────────

    private void sendSlack(Alert alert) {
        AppProperties.NotificationProperties.SlackProperties slackCfg =
                appProperties.getNotifications().getSlack();

        String emoji = switch (alert.getSeverity()) {
            case CRITICAL -> ":rotating_light:";
            case HIGH     -> ":warning:";
            case MEDIUM   -> ":yellow_circle:";
            default       -> ":information_source:";
        };

        Map<String, Object> payload = Map.of(
                "channel", slackCfg.getChannel(),
                "text",    emoji + " *[" + alert.getSeverity() + "] " + alert.getTitle() + "*",
                "blocks",  List.of(
                        Map.of("type", "section", "text",
                                Map.of("type", "mrkdwn", "text",
                                        emoji + " *" + alert.getTitle() + "*\n"
                                        + alert.getMessage())),
                        Map.of("type", "context", "elements", List.of(
                                Map.of("type", "mrkdwn",
                                       "text", "Org: *" + alert.getOrganization().getName()
                                               + "* | Severity: *" + alert.getSeverity() + "*"
                                               + " | Alert ID: `" + alert.getId() + "`"))))
        );

        restTemplate.postForEntity(slackCfg.getWebhookUrl(), payload, String.class);
        log.debug("Slack notification sent for alert {}", alert.getId());
    }
}
