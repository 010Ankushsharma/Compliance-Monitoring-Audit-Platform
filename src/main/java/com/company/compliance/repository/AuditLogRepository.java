package com.company.compliance.repository;

import com.company.compliance.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link AuditLog} entities.
 *
 * <p><strong>No @Modifying methods exist here.</strong>
 * Audit logs are immutable — the DB trigger and entity lifecycle hooks
 * both reject UPDATE and DELETE. This repository is intentionally insert+read only.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // ── Chain integrity ───────────────────────────────────────────

    /** Fetch the latest entry for building the next hash link. */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.organization.id = :orgId
            ORDER BY a.sequenceNumber DESC
            LIMIT 1
            """)
    Optional<AuditLog> findLatestByOrganization(@Param("orgId") UUID organizationId);

    /** Fetch entire chain in sequence order for verification. */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.organization.id = :orgId
              AND (:from IS NULL OR a.timestamp >= :from)
              AND (:to   IS NULL OR a.timestamp <= :to)
            ORDER BY a.sequenceNumber ASC
            """)
    List<AuditLog> findChainForVerification(
            @Param("orgId") UUID organizationId,
            @Param("from")  OffsetDateTime from,
            @Param("to")    OffsetDateTime to);

    // ── Dynamic search ────────────────────────────────────────────

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.organization.id = :orgId
              AND (:userId    IS NULL OR a.user.id      = :userId)
              AND (:action    IS NULL OR a.action        = :action)
              AND (:resType   IS NULL OR a.resourceType  = :resType)
              AND (:resId     IS NULL OR a.resourceId    = :resId)
              AND (:outcome   IS NULL OR a.outcome       = :outcome)
              AND (:requestId IS NULL OR a.requestId     = :requestId)
              AND (:ipAddress IS NULL OR a.ipAddress     = :ipAddress)
              AND (:from      IS NULL OR a.timestamp    >= :from)
              AND (:to        IS NULL OR a.timestamp    <= :to)
            ORDER BY a.timestamp DESC
            """)
    Page<AuditLog> search(
            @Param("orgId")      UUID organizationId,
            @Param("userId")     UUID userId,
            @Param("action")     String action,
            @Param("resType")    String resourceType,
            @Param("resId")      String resourceId,
            @Param("outcome")    String outcome,
            @Param("requestId")  String requestId,
            @Param("ipAddress")  String ipAddress,
            @Param("from")       OffsetDateTime from,
            @Param("to")         OffsetDateTime to,
            Pageable pageable);

    // ── Multi-action filter (used by export) ──────────────────────

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.organization.id = :orgId
              AND a.action IN :actions
              AND a.timestamp BETWEEN :from AND :to
            ORDER BY a.timestamp DESC
            """)
    List<AuditLog> findByActionsAndTimeRange(
            @Param("orgId")    UUID organizationId,
            @Param("actions")  List<String> actions,
            @Param("from")     OffsetDateTime from,
            @Param("to")       OffsetDateTime to);

    // ── User activity ─────────────────────────────────────────────

    Page<AuditLog> findAllByUserIdOrderByTimestampDesc(UUID userId, Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.user.id = :userId
              AND a.timestamp >= :since
            ORDER BY a.timestamp DESC
            """)
    List<AuditLog> findRecentActivityByUser(
            @Param("userId") UUID userId,
            @Param("since")  OffsetDateTime since);

    // ── Failure detection ─────────────────────────────────────────

    /**
     * Returns failed login attempts from an IP within the lookback window.
     * Used by the rate-limiter and fraud detection components.
     */
    @Query("""
            SELECT COUNT(a) FROM AuditLog a
            WHERE a.action    = 'LOGIN'
              AND a.outcome   = 'FAILURE'
              AND a.ipAddress = :ip
              AND a.timestamp >= :since
            """)
    long countFailedLoginsByIp(
            @Param("ip")    String ipAddress,
            @Param("since") OffsetDateTime since);

    // ── Statistics ────────────────────────────────────────────────

    @Query("""
            SELECT a.action, COUNT(a) FROM AuditLog a
            WHERE a.organization.id = :orgId
              AND a.timestamp BETWEEN :from AND :to
            GROUP BY a.action
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countByActionInPeriod(
            @Param("orgId") UUID organizationId,
            @Param("from")  OffsetDateTime from,
            @Param("to")    OffsetDateTime to);

    @Query("""
            SELECT DATE(a.timestamp), COUNT(a) FROM AuditLog a
            WHERE a.organization.id = :orgId
              AND a.timestamp BETWEEN :from AND :to
            GROUP BY DATE(a.timestamp)
            ORDER BY DATE(a.timestamp) ASC
            """)
    List<Object[]> countDailyInPeriod(
            @Param("orgId") UUID organizationId,
            @Param("from")  OffsetDateTime from,
            @Param("to")    OffsetDateTime to);

    long countByOrganizationId(UUID organizationId);

    // ── Retention cleanup (native — bypasses immutability trigger) ─

    /**
     * Hard-deletes audit logs older than the retention cutoff.
     *
     * <p><strong>This is the ONLY permitted deletion path.</strong>
     * It is executed by the {@code AuditRetentionScheduler} with SUPER_ADMIN context
     * and requires the {@code COMPLIANCE_DATA_RETENTION} database role which bypasses
     * the immutability trigger.
     */
    @Query(value = """
            DELETE FROM audit_logs
            WHERE organization_id = :orgId
              AND timestamp < :cutoff
            """, nativeQuery = true)
    int deleteByOrganizationAndOlderThan(
            @Param("orgId")   UUID organizationId,
            @Param("cutoff")  OffsetDateTime cutoff);
}
