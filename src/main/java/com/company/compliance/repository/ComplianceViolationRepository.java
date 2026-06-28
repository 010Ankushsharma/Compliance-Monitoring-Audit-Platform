package com.company.compliance.repository;

import com.company.compliance.domain.entity.ComplianceViolation;
import com.company.compliance.domain.enums.Severity;
import com.company.compliance.domain.enums.ViolationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ComplianceViolation} entities.
 */
@Repository
public interface ComplianceViolationRepository
        extends JpaRepository<ComplianceViolation, UUID>,
                JpaSpecificationExecutor<ComplianceViolation> {

    // ── Lookup ────────────────────────────────────────────────────

    Optional<ComplianceViolation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    // ── Open violations for risk scoring ─────────────────────────

    /**
     * All open (risk-score-affecting) violations for a given policy.
     * Called by {@code RiskScoringService} on each evaluation run.
     */
    @Query("""
            SELECT v FROM ComplianceViolation v
            WHERE v.policy.id = :policyId
              AND v.status IN ('OPEN', 'IN_REVIEW')
            ORDER BY v.severity DESC, v.detectedAt DESC
            """)
    List<ComplianceViolation> findOpenByPolicy(@Param("policyId") UUID policyId);

    @Query("""
            SELECT v FROM ComplianceViolation v
            WHERE v.organization.id = :orgId
              AND v.status IN ('OPEN', 'IN_REVIEW')
            ORDER BY v.severity DESC, v.detectedAt DESC
            """)
    List<ComplianceViolation> findOpenByOrganization(@Param("orgId") UUID organizationId);

    // ── Dynamic filter (paginated) ────────────────────────────────

    @Query("""
            SELECT v FROM ComplianceViolation v
            WHERE v.organization.id = :orgId
              AND (:policyId  IS NULL OR v.policy.id    = :policyId)
              AND (:userId    IS NULL OR v.user.id      = :userId)
              AND (:severity  IS NULL OR v.severity     = :severity)
              AND (:status    IS NULL OR v.status       = :status)
              AND (:framework IS NULL OR v.policy.framework = :framework)
              AND (:from      IS NULL OR v.detectedAt   >= :from)
              AND (:to        IS NULL OR v.detectedAt   <= :to)
            """)
    Page<ComplianceViolation> findWithFilters(
            @Param("orgId")      UUID organizationId,
            @Param("policyId")   UUID policyId,
            @Param("userId")     UUID userId,
            @Param("severity")   Severity severity,
            @Param("status")     ViolationStatus status,
            @Param("framework")  String framework,
            @Param("from")       OffsetDateTime from,
            @Param("to")         OffsetDateTime to,
            Pageable pageable);

    // ── Duplicate detection (dedup before creating new violation) ─

    /**
     * Checks whether an open violation already exists for the same
     * policy rule and user within the grace period window.
     */
    @Query("""
            SELECT COUNT(v) > 0 FROM ComplianceViolation v
            WHERE v.policyRule.id = :ruleId
              AND v.user.id       = :userId
              AND v.status IN ('OPEN', 'IN_REVIEW')
              AND v.detectedAt   >= :since
            """)
    boolean existsOpenViolationInGracePeriod(
            @Param("ruleId") UUID policyRuleId,
            @Param("userId") UUID userId,
            @Param("since")  OffsetDateTime since);

    // ── Dashboard summary counts ──────────────────────────────────

    @Query("""
            SELECT v.severity, COUNT(v) FROM ComplianceViolation v
            WHERE v.organization.id = :orgId
              AND v.status IN ('OPEN', 'IN_REVIEW')
            GROUP BY v.severity
            """)
    List<Object[]> countOpenBySeverity(@Param("orgId") UUID organizationId);

    @Query("""
            SELECT v.policy.framework, COUNT(v) FROM ComplianceViolation v
            WHERE v.organization.id = :orgId
              AND v.status IN ('OPEN', 'IN_REVIEW')
            GROUP BY v.policy.framework
            """)
    List<Object[]> countOpenByFramework(@Param("orgId") UUID organizationId);

    @Query("""
            SELECT v.policy.id, v.policy.name, COUNT(v) FROM ComplianceViolation v
            WHERE v.organization.id = :orgId
              AND v.status IN ('OPEN', 'IN_REVIEW')
            GROUP BY v.policy.id, v.policy.name
            ORDER BY COUNT(v) DESC
            """)
    List<Object[]> countOpenByPolicy(@Param("orgId") UUID organizationId);

    long countByOrganizationIdAndStatus(UUID organizationId, ViolationStatus status);

    long countByOrganizationIdAndStatusIn(UUID organizationId, List<ViolationStatus> statuses);

    // ── Average resolution time ───────────────────────────────────

    @Query(value = """
            SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - detected_at)) / 3600.0)
            FROM compliance_violations
            WHERE organization_id = :orgId
              AND status IN ('RESOLVED', 'FALSE_POSITIVE')
              AND resolved_at IS NOT NULL
              AND detected_at >= :since
            """, nativeQuery = true)
    Double averageResolutionHours(
            @Param("orgId") UUID organizationId,
            @Param("since") OffsetDateTime since);

    // ── Trend (daily counts for charts) ──────────────────────────

    @Query(value = """
            SELECT DATE(detected_at) AS day, severity, COUNT(*) AS cnt
            FROM compliance_violations
            WHERE organization_id = :orgId
              AND detected_at BETWEEN :from AND :to
            GROUP BY DATE(detected_at), severity
            ORDER BY day ASC
            """, nativeQuery = true)
    List<Object[]> dailyViolationTrend(
            @Param("orgId") UUID organizationId,
            @Param("from")  OffsetDateTime from,
            @Param("to")    OffsetDateTime to);

    // ── Most recent violations (for dashboard tiles) ──────────────

    @Query("""
            SELECT v FROM ComplianceViolation v
            WHERE v.organization.id = :orgId
              AND v.status IN ('OPEN', 'IN_REVIEW')
            ORDER BY v.severity DESC, v.detectedAt DESC
            LIMIT :limit
            """)
    List<ComplianceViolation> findMostCriticalOpen(
            @Param("orgId")  UUID organizationId,
            @Param("limit")  int limit);
}
