package com.company.compliance.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * An individual evaluation rule within a {@link Policy}.
 *
 * <p>Rules are evaluated in ascending {@code evaluationOrder}. The rule
 * defines a field to inspect, an operator, and an expected value.
 * The violation engine compares actual runtime values against these.
 *
 * <p>Maps to the {@code policy_rules} table.
 */
@Entity
@Table(name = "policy_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "policy")
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Rule evaluation strategy.
     * Values: THRESHOLD, PATTERN, PRESENCE, FREQUENCY, CUSTOM
     */
    @Column(name = "rule_type", nullable = false, length = 30)
    private String ruleType;

    /** The data field path to evaluate (e.g. {@code user.mfa_enabled}). */
    @Column(name = "field", nullable = false, length = 255)
    private String field;

    /**
     * Comparison operator.
     * Values: EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN,
     *         CONTAINS, NOT_CONTAINS, MATCHES_REGEX, IN, NOT_IN
     */
    @Column(name = "operator", nullable = false, length = 20)
    private String operator;

    /** Expected value (right-hand side of the operator comparison). */
    @Column(name = "value", nullable = false, columnDefinition = "TEXT")
    private String value;

    /** Number of days before a new violation is raised after initial detection. */
    @Column(name = "grace_period_days", nullable = false)
    @Builder.Default
    private int gracePeriodDays = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Lower number = evaluated first within the policy. */
    @Column(name = "evaluation_order", nullable = false)
    @Builder.Default
    private short evaluationOrder = 0;

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
}
