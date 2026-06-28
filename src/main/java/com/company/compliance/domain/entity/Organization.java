package com.company.compliance.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Root tenant entity. Every other entity is scoped to an Organization.
 *
 * <p>Maps to the {@code organizations} table created in V1__init_schema.sql.
 */
@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"users", "policies"})
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "industry", length = 100)
    private String industry;

    @Column(name = "country", length = 100)
    private String country;

    /**
     * Regulatory frameworks this organisation is subject to.
     * Stored as a PostgreSQL text array.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "regulatory_frameworks", columnDefinition = "text[]")
    @Builder.Default
    private List<String> regulatoryFrameworks = new ArrayList<>();

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "overall_risk_score", precision = 5, scale = 2)
    private BigDecimal overallRiskScore;

    @Column(name = "risk_last_updated")
    private OffsetDateTime riskLastUpdated;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // ── Relationships ─────────────────────────────────────────────

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Policy> policies = new ArrayList<>();

    // ── Lifecycle ─────────────────────────────────────────────────

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Domain helpers ────────────────────────────────────────────

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.active = false;
    }
}
