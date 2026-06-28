package com.company.compliance.repository;

import com.company.compliance.domain.entity.Alert;
import com.company.compliance.domain.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Alert} entities.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    // ── Lookup ────────────────────────────────────────────────────

    Optional<Alert> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /** Deduplication check — prevents duplicate alerts for the same event. */
    Optional<Alert> findByDedupKeyAndStatusNot(String dedupKey, String status);

    boolean existsByDedupKeyAndStatusNot(String dedupKey, String status);

    // ── Open alerts ───────────────────────────────────────────────

    @Query("""
            SELECT a FROM Alert a
            WHERE a.organization.id = :orgId
              AND a.status = 'OPEN'
            ORDER BY a.severity DESC, a.createdAt DESC
            """)
    List<Alert> findOpenByOrganization(@Param("orgId") UUID organizationId);

    @Query("""
            SELECT a FROM Alert a
            WHERE a.organization.id = :orgId
              AND a.status = 'OPEN'
            ORDER BY a.severity DESC, a.createdAt DESC
            """)
    Page<Alert> findOpenByOrganization(@Param("orgId") UUID organizationId, Pageable pageable);

    // ── Notification queue ────────────────────────────────────────

    /**
     * Finds open alerts that have not yet had notifications sent.
     * Polled by the {@code NotificationScheduler} every 30 seconds.
     */
    @Query("""
            SELECT a FROM Alert a
            WHERE a.status = 'OPEN'
              AND a.notificationSent = false
              AND (a.suppressedUntil IS NULL OR a.suppressedUntil <= :now)
            ORDER BY a.severity DESC, a.createdAt ASC
            LIMIT :batchSize
            """)
    List<Alert> findPendingNotifications(
            @Param("now")       OffsetDateTime now,
            @Param("batchSize") int batchSize);

    // ── Escalation ────────────────────────────────────────────────

    /**
     * Returns alerts that have been open for too long and need escalation.
     * Thresholds: CRITICAL = 1h, HIGH = 4h, others = 24h.
     */
    @Query("""
            SELECT a FROM Alert a
            WHERE a.status = 'OPEN'
              AND a.escalationLevel < :maxLevel
              AND (
                    (a.severity = 'CRITICAL' AND a.createdAt <= :criticalThreshold)
                 OR (a.severity = 'HIGH'     AND a.createdAt <= :highThreshold)
                 OR (a.severity NOT IN ('CRITICAL','HIGH') AND a.createdAt <= :defaultThreshold)
              )
            ORDER BY a.severity DESC, a.createdAt ASC
            """)
    List<Alert> findAlertsRequiringEscalation(
            @Param("maxLevel")          int maxEscalationLevel,
            @Param("criticalThreshold") OffsetDateTime criticalThreshold,
            @Param("highThreshold")     OffsetDateTime highThreshold,
            @Param("defaultThreshold")  OffsetDateTime defaultThreshold);

    // ── Filtered list (paginated) ─────────────────────────────────

    @Query("""
            SELECT a FROM Alert a
            WHERE a.organization.id = :orgId
              AND (:status   IS NULL OR a.status   = :status)
              AND (:severity IS NULL OR a.severity = :severity)
              AND (:from     IS NULL OR a.createdAt >= :from)
              AND (:to       IS NULL OR a.createdAt <= :to)
            """)
    Page<Alert> findWithFilters(
            @Param("orgId")    UUID organizationId,
            @Param("status")   String status,
            @Param("severity") Severity severity,
            @Param("from")     OffsetDateTime from,
            @Param("to")       OffsetDateTime to,
            Pageable pageable);

    // ── Bulk suppress ─────────────────────────────────────────────

    @Modifying
    @Query("""
            UPDATE Alert a
            SET a.status          = 'SUPPRESSED',
                a.suppressedUntil = :suppressUntil
            WHERE a.organization.id = :orgId
              AND a.status = 'OPEN'
              AND a.severity NOT IN ('CRITICAL')
            """)
    int suppressNonCriticalAlerts(
            @Param("orgId")         UUID organizationId,
            @Param("suppressUntil") OffsetDateTime suppressUntil);

    // ── Statistics ────────────────────────────────────────────────

    long countByOrganizationIdAndStatus(UUID organizationId, String status);

    @Query("""
            SELECT a.severity, COUNT(a) FROM Alert a
            WHERE a.organization.id = :orgId
              AND a.status = 'OPEN'
            GROUP BY a.severity
            """)
    List<Object[]> countOpenBySeverity(@Param("orgId") UUID organizationId);

    @Query("""
            SELECT a FROM Alert a
            WHERE a.organization.id = :orgId
            ORDER BY a.createdAt DESC
            LIMIT :limit
            """)
    List<Alert> findRecent(
            @Param("orgId")  UUID organizationId,
            @Param("limit")  int limit);
}
