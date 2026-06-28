package com.company.compliance.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Per-organisation notification channel configuration.
 * Maps to the {@code notification_channels} table (V1).
 *
 * <p>File: {@code src/main/java/com/company/compliance/domain/entity/NotificationChannel.java}
 */
@Entity
@Table(name = "notification_channels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class NotificationChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Values: EMAIL, SLACK, WEBHOOK, SMS, PAGERDUTY */
    @Column(name = "channel_type", nullable = false, length = 30)
    private String channelType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Encrypted channel config — encrypted at app layer before insert. */
    @Type(JsonType.class)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config;

    /** Minimum severity that triggers this channel. */
    @Column(name = "min_severity", nullable = false, length = 20)
    @Builder.Default
    private String minSeverity = "HIGH";

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }
}
