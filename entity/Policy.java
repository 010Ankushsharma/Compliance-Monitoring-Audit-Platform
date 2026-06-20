package com.company.compliance.domain.entity;

import com.company.compliance.domain.enums.PolicyStatus;
import com.company.compliance.domain.enums.RegulatoryFramework;
import com.company.compliance.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A compliance policy aligned to a regulatory framework.
 *
 * <p>A policy contains one or more {@link PolicyRule}s that are evaluated
 * on each scheduled or manual evaluation run. Only {@code ACTIVE} policies
 * are included in violation detection.
 *
 * <p>Maps to the {@code policies} table.
 */
@Entity
@Table(name = "policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"organization", "rules", "createdBy", "owner"})
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "framework", nullable = false, length = 50)
    private RegulatoryFramework framework;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.DRAFT;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "evaluation_schedule", length = 100)
    @Builder.Default
    private String evaluationSchedule = "0 0 * * *"; // daily midnight (cron)

    @Column(name = "last_evaluated_at")
    private OffsetDateTime lastEvaluatedAt;

    @Column(name = "next_evaluation_at")
    private OffsetDateTime nextEvaluatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // ── Relationships ─────────────────────────────────────────────

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("evaluationOrder ASC")
    @Builder.Default
    private List<PolicyRule> rules = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Domain helpers ────────────────────────────────────────────

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isEvaluable() {
        return status != null && status.isEvaluable() && !isDeleted();
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public void addRule(PolicyRule rule) {
        rules.add(rule);
        rule.setPolicy(this);
    }

    public void removeRule(PolicyRule rule) {
        rules.remove(rule);
        rule.setPolicy(null);
    }

    /** Increment version on every content-altering update. */
    public void bumpVersion() {
        this.version++;
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.status = PolicyStatus.ARCHIVED;
    }
}
