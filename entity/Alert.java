package com.company.compliance.domain.entity;

import com.company.compliance.domain.enums.Severity;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * An actionable alert derived from a compliance violation or monitoring rule.
 *
 * <p>Alerts support deduplication via {@code dedupKey}, escalation tracking,
 * and multi-channel notification state.
 *
 * <p>Maps to the {@code alerts} table.
 */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"organization", "violation", "acknowledgedBy", "resolvedBy"})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "violation_id")
    private ComplianceViolation violation;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    /** Alert workflow status. Values: OPEN, ACKNOWLEDGED, RESOLVED, SUPPRESSED. */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "source", length = 100)
    private String source;

    /** Deterministic key for deduplication — prevents duplicate alert storms. */
    @Column(name = "dedup_key", length = 255)
    private String dedupKey;

    @Column(name = "suppressed_until")
    private OffsetDateTime suppressedUntil;

    @Column(name = "escalation_level", nullable = false)
    @Builder.Default
    private short escalationLevel = 0;

    @Column(name = "escalated_at")
    private OffsetDateTime escalatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "notification_sent", nullable = false)
    @Builder.Default
    private boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private OffsetDateTime notificationSentAt;

    @Type(StringArrayType.class)
    @Column(name = "notification_channels", columnDefinition = "text[]")
    @Builder.Default
    private String[] notificationChannels = new String[]{};

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // ── Lifecycle ─────────────────────────────────────────────────

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Domain helpers ────────────────────────────────────────────

    public boolean isOpen() {
        return "OPEN".equals(status);
    }

    public boolean isSuppressed() {
        return "SUPPRESSED".equals(status)
                || (suppressedUntil != null && suppressedUntil.isAfter(OffsetDateTime.now()));
    }

    public void markNotificationSent(String[] channels) {
        this.notificationSent = true;
        this.notificationSentAt = OffsetDateTime.now();
        this.notificationChannels = channels;
    }

    public void acknowledge(User actor) {
        this.acknowledgedBy = actor;
        this.acknowledgedAt = OffsetDateTime.now();
        this.status = "ACKNOWLEDGED";
    }

    public void resolve(User actor) {
        this.resolvedBy = actor;
        this.resolvedAt = OffsetDateTime.now();
        this.status = "RESOLVED";
    }

    public void escalate() {
        this.escalationLevel++;
        this.escalatedAt = OffsetDateTime.now();
    }
}
