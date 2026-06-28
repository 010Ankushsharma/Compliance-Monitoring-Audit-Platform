package com.company.compliance.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Audit trail of a single policy evaluation run (scheduled or manual).
 *
 * <p>Maps to the {@code policy_evaluations} table (added in V3).
 */
@Entity
@Table(name = "policy_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"organization", "policy", "triggeredBy"})
public class PolicyEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by")
    private User triggeredBy;

    /** Values: SCHEDULED, MANUAL, EVENT_DRIVEN */
    @Column(name = "trigger_type", nullable = false, length = 20)
    @Builder.Default
    private String triggerType = "SCHEDULED";

    /** Values: RUNNING, COMPLETED, FAILED, CANCELLED */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "RUNNING";

    @Column(name = "rules_evaluated", nullable = false)
    @Builder.Default
    private int rulesEvaluated = 0;

    @Column(name = "violations_found", nullable = false)
    @Builder.Default
    private int violationsFound = 0;

    @Column(name = "violations_new", nullable = false)
    @Builder.Default
    private int violationsNew = 0;

    @Column(name = "started_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Type(JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }

    public void complete(int rulesEvaluated, int violationsFound, int violationsNew) {
        this.status = "COMPLETED";
        this.rulesEvaluated = rulesEvaluated;
        this.violationsFound = violationsFound;
        this.violationsNew = violationsNew;
        this.completedAt = OffsetDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = OffsetDateTime.now();
    }
}
