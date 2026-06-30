package com.company.compliance.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * Strongly-typed configuration properties bound from {@code application.yml}
 * under the {@code app:} prefix.
 *
 * <p>All required fields are validated at startup via {@code @Validated}.
 * The application will refuse to start if any constraint fails — preventing
 * silent misconfiguration in production.
 */
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    // ── JWT ───────────────────────────────────────────────────────

    @Valid
    private JwtProperties jwt = new JwtProperties();

    @Data
    public static class JwtProperties {

        @NotBlank(message = "app.jwt.secret must not be blank")
        @Size(min = 32, message = "app.jwt.secret must be at least 256 bits (32 chars)")
        private String secret;

        @Positive(message = "app.jwt.expiration-ms must be positive")
        private long expirationMs = 3_600_000L;            // 1 hour default

        @Positive(message = "app.jwt.refresh-expiration-ms must be positive")
        private long refreshExpirationMs = 604_800_000L;   // 7 days default

        @NotBlank
        private String issuer = "compliance-audit-platform";
    }

    // ── Audit ─────────────────────────────────────────────────────

    @Valid
    private AuditProperties audit = new AuditProperties();

    @Data
    public static class AuditProperties {

        @Min(value = 1, message = "Retention days must be at least 1")
        @Max(value = 3650, message = "Retention days cannot exceed 10 years")
        private int retentionDays = 365;

        private String hashAlgorithm = "SHA-256";

        private boolean chainVerificationEnabled = true;

        /** When true, audit events are published via Kafka asynchronously. */
        private boolean asyncEnabled = true;
    }

    // ── Rate Limiting ─────────────────────────────────────────────

    @Valid
    private RateLimitProperties rateLimit = new RateLimitProperties();

    @Data
    public static class RateLimitProperties {

        private boolean enabled = true;

        @Positive(message = "Requests-per-minute must be positive")
        private long requestsPerMinute = 60L;

        @Positive(message = "Burst capacity must be positive")
        private long burstCapacity = 20L;
    }

    // ── Kafka Topic Names ─────────────────────────────────────────

    @Valid
    private KafkaTopicProperties kafka = new KafkaTopicProperties();

    @Data
   public static class KafkaTopicProperties {

    private String auditEvents     = "compliance.audit-events";
    private String violations      = "compliance.violations";
    private String alerts          = "compliance.alerts";
    private String reportRequests  = "compliance.report-requests";

    @Min(1) private int partitions         = 3;
    @Min(1) private int replicationFactor  = 1;

    private String transactionalIdPrefix = "";
}

    // ── Notifications ─────────────────────────────────────────────

    @Valid
    private NotificationProperties notifications = new NotificationProperties();

    @Data
    public static class NotificationProperties {

        @Valid
        private EmailProperties email   = new EmailProperties();

        @Valid
        private SlackProperties slack   = new SlackProperties();

        @Valid
        private WebhookProperties webhook = new WebhookProperties();

        @Data
        public static class EmailProperties {
            private String  from        = "noreply@company.com";
            private String  fromName    = "Compliance Platform";
            private boolean enabled     = false;
        }

        @Data
        public static class SlackProperties {
            private String  webhookUrl  = "";
            private boolean enabled     = false;
            private String  channel     = "#compliance-alerts";
        }

        @Data
        public static class WebhookProperties {
            @Positive private int timeoutSeconds   = 10;
            @Positive private int retryAttempts    = 3;
        }
    }

    // ── Reports ───────────────────────────────────────────────────

    @Valid
    private ReportProperties reports = new ReportProperties();

    @Data
    public static class ReportProperties {

        @NotBlank
        private String storagePath          = "/tmp/compliance-reports";

        private boolean asyncGeneration     = true;

        @Positive @Max(50)
        private int maxConcurrentJobs       = 5;

        @Positive
        private int cleanupDays             = 90;
    }

    // ── CORS ──────────────────────────────────────────────────────

    @Valid
    private CorsProperties cors = new CorsProperties();

    @Data
    public static class CorsProperties {

        private String allowedOrigins  = "http://localhost:3000";
        private String allowedMethods  = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
        private String allowedHeaders  = "*";
        private boolean allowCredentials = true;
        private long maxAge            = 3600L;

        public List<String> allowedOriginList() {
            return List.of(allowedOrigins.split(","));
        }

        public List<String> allowedMethodList() {
            return List.of(allowedMethods.split(","));
        }
    }

    // ── Security ──────────────────────────────────────────────────

    @Valid
    private SecurityProperties security = new SecurityProperties();

    @Data
    public static class SecurityProperties {

        private boolean ipAllowlistEnabled  = false;
        private String  allowedIps          = "";

        public List<String> allowedIpList() {
            if (allowedIps == null || allowedIps.isBlank()) return new ArrayList<>();
            return List.of(allowedIps.split(","));
        }
    }
}
