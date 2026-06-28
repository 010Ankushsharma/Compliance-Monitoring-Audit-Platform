package com.company.compliance.repository;

import com.company.compliance.domain.entity.Report;
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
 * Repository for {@link Report} entities.
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    // ── Lookup ────────────────────────────────────────────────────

    Optional<Report> findByIdAndOrganizationId(UUID id, UUID organizationId);

    // ── Organisation-scoped list ──────────────────────────────────

    Page<Report> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    @Query("""
            SELECT r FROM Report r
            WHERE r.organization.id = :orgId
              AND (:status IS NULL OR r.status = :status)
              AND (:format IS NULL OR r.format = :format)
            ORDER BY r.createdAt DESC
            """)
    Page<Report> findWithFilters(
            @Param("orgId")   UUID organizationId,
            @Param("status")  String status,
            @Param("format")  String format,
            Pageable pageable);

    // ── Pending / stuck jobs ──────────────────────────────────────

    /**
     * Finds PENDING reports that have not been picked up by a consumer.
     * Used by the report recovery scheduler.
     */
    @Query("""
            SELECT r FROM Report r
            WHERE r.status = 'PENDING'
              AND r.createdAt <= :stuckSince
            ORDER BY r.createdAt ASC
            """)
    List<Report> findStuckPendingReports(@Param("stuckSince") OffsetDateTime stuckSince);

    @Query("""
            SELECT r FROM Report r
            WHERE r.status = 'GENERATING'
              AND r.startedAt <= :stuckSince
            ORDER BY r.startedAt ASC
            """)
    List<Report> findStuckGeneratingReports(@Param("stuckSince") OffsetDateTime stuckSince);

    // ── Cleanup ───────────────────────────────────────────────────

    @Query("""
            SELECT r FROM Report r
            WHERE r.status = 'COMPLETED'
              AND r.completedAt <= :cutoff
            """)
    List<Report> findReportsForCleanup(@Param("cutoff") OffsetDateTime cutoff);

    // ── Statistics ────────────────────────────────────────────────

    long countByOrganizationIdAndStatus(UUID organizationId, String status);

    @Query("""
            SELECT r.templateId, COUNT(r) FROM Report r
            WHERE r.organization.id = :orgId
              AND r.createdAt >= :since
            GROUP BY r.templateId
            ORDER BY COUNT(r) DESC
            """)
    List<Object[]> countByTemplateForOrganizationSince(
            @Param("orgId") UUID organizationId,
            @Param("since") OffsetDateTime since);
}
