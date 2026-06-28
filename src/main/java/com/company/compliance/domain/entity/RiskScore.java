package com.company.compliance.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Per-policy rolling risk/compliance score.
 * Maps to the {@code risk_scores} table (added in V3).
 *
 * <p>File: {@code src/main/java/com/company/compliance/domain/entity/RiskScore.java}
 */
@Entity
@Table(name = "risk_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RiskScore {

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

    @Column(name = "framework", nullable = false, length = 50)
    private String framework;

    @Column(name = "compliance_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal complianceScore = BigDecimal.ZERO;

    @Column(name = "violation_penalty", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal violationPenalty = BigDecimal.ZERO;

    @Column(name = "open_violations_count", nullable = false)
    @Builder.Default
    private int openViolationsCount = 0;

    @Column(name = "critical_violations", nullable = false)
    @Builder.Default
    private int criticalViolations = 0;

    @Column(name = "high_violations", nullable = false)
    @Builder.Default
    private int highViolations = 0;

    @Column(name = "medium_violations", nullable = false)
    @Builder.Default
    private int mediumViolations = 0;

    @Column(name = "low_violations", nullable = false)
    @Builder.Default
    private int lowViolations = 0;

    @Column(name = "previous_score", precision = 5, scale = 2)
    private BigDecimal previousScore;

    @Column(name = "trend", length = 10)
    private String trend;

    @Column(name = "last_evaluated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime lastEvaluatedAt = OffsetDateTime.now();

    @Column(name = "next_evaluation_at")
    private OffsetDateTime nextEvaluationAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    protected void onUpdate() { this.updatedAt = OffsetDateTime.now(); }
}
