package com.company.compliance.domain.entity;

import com.company.compliance.domain.enums.Severity;
import com.company.compliance.domain.enums.ViolationStatus;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A policy violation detected by the violation engine.
 *
 * <p>Lifecycle: OPEN → IN_REVIEW → RESOLVED | FALSE_POSITIVE | SUPPRESSED.
 * Status transitions are validated via {@link ViolationStatus#canTransitionTo}.
 *
 * <p>Maps to the {@code compliance_violations} table.
 */
@Entity
@Table(name = "compliance_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"policy", "policyRule", "auditLog", "user",
                     "acknowledgedBy", "resolvedBy", "evidenceAttachments"})
public class ComplianceViolation {

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
    @JoinColumn(name = "policy_rule_id")
    private PolicyRule policyRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_log_id")
    private AuditLog auditLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_evaluation_id")
    private PolicyEvaluation policyEvaluation;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ViolationStatus status = ViolationStatus.OPEN;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** JSON snapshot of the context captured at detection time. */
    @Type(JsonType.class)
    @Column(name = "evidence", columnDefinition = "jsonb")
    private Map<String, Object> evidence;

    @Column(name = "detected_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime detectedAt = OffsetDateTime.now();

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

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // ── Relationships ─────────────────────────────────────────────

    @OneToMany(mappedBy = "violation", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EvidenceAttachment> evidenceAttachments = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Domain helpers ────────────────────────────────────────────

    public boolean isOpen() {
        return status != null && status.isActive();
    }

    public boolean affectsRiskScore() {
        return status != null && status.isAffectsRiskScore();
    }

    /**
     * Acknowledge this violation. Validates status transition.
     *
     * @param actor the user acknowledging the violation
     * @throws IllegalStateException if transition is not permitted
     */
    public void acknowledge(User actor) {
        if (!ViolationStatus.OPEN.canTransitionTo(ViolationStatus.IN_REVIEW)) {
            throw new IllegalStateException(
                    "Cannot acknowledge violation in status: " + status);
        }
        this.acknowledgedBy = actor;
        this.acknowledgedAt = OffsetDateTime.now();
        this.status = ViolationStatus.IN_REVIEW;
    }

    /**
     * Resolve this violation with a resolution note.
     *
     * @param actor          user performing the resolution
     * @param resolutionNote what was done to fix it
     */
    public void resolve(User actor, String resolutionNote) {
        if (!status.canTransitionTo(ViolationStatus.RESOLVED)) {
            throw new IllegalStateException(
                    "Cannot resolve violation in status: " + status);
        }
        this.resolvedBy = actor;
        this.resolvedAt = OffsetDateTime.now();
        this.resolutionNote = resolutionNote;
        this.status = ViolationStatus.RESOLVED;
    }

    public void markFalsePositive(User actor, String note) {
        if (!status.canTransitionTo(ViolationStatus.FALSE_POSITIVE)) {
            throw new IllegalStateException(
                    "Cannot mark as false positive in status: " + status);
        }
        this.resolvedBy = actor;
        this.resolvedAt = OffsetDateTime.now();
        this.resolutionNote = note;
        this.status = ViolationStatus.FALSE_POSITIVE;
    }
}
