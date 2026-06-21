package com.company.compliance.repository;

import com.company.compliance.domain.entity.Policy;
import com.company.compliance.domain.enums.PolicyStatus;
import com.company.compliance.domain.enums.RegulatoryFramework;
import com.company.compliance.domain.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Policy} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering
 * via {@code PolicySpecification} in the service layer.
 */
@Repository
public interface PolicyRepository
        extends JpaRepository<Policy, UUID>, JpaSpecificationExecutor<Policy> {

    // ── Lookup ────────────────────────────────────────────────────

    Optional<Policy> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Policy> findByIdAndOrganizationIdAndDeletedAtIsNull(UUID id, UUID organizationId);

    boolean existsByNameAndOrganizationIdAndDeletedAtIsNull(String name, UUID organizationId);

    boolean existsByNameAndOrganizationIdAndIdNotAndDeletedAtIsNull(
            String name, UUID organizationId, UUID excludeId);

    // ── Organisation-scoped queries ───────────────────────────────

    Page<Policy> findAllByOrganizationIdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    @Query("""
            SELECT p FROM Policy p
            WHERE p.organization.id = :orgId
              AND p.deletedAt IS NULL
              AND (:status IS NULL OR p.status = :status)
              AND (:framework IS NULL OR p.framework = :framework)
              AND (:severity IS NULL OR p.severity = :severity)
            """)
    Page<Policy> findWithFilters(
            @Param("orgId")      UUID organizationId,
            @Param("status")     PolicyStatus status,
            @Param("framework")  RegulatoryFramework framework,
            @Param("severity")   Severity severity,
            Pageable pageable);

    // ── Evaluable policies ────────────────────────────────────────

    /**
     * Returns all ACTIVE policies due for scheduled evaluation.
     * Used by the scheduler every minute to find work.
     */
    @Query("""
            SELECT p FROM Policy p
            WHERE p.status = 'ACTIVE'
              AND p.deletedAt IS NULL
              AND (p.nextEvaluatedAt IS NULL OR p.nextEvaluatedAt <= :now)
            ORDER BY p.nextEvaluatedAt ASC NULLS FIRST
            """)
    List<Policy> findPoliciesDueForEvaluation(@Param("now") OffsetDateTime now);

    List<Policy> findAllByOrganizationIdAndStatusAndDeletedAtIsNull(
            UUID organizationId, PolicyStatus status);

    List<Policy> findAllByOrganizationIdAndFrameworkAndDeletedAtIsNull(
            UUID organizationId, RegulatoryFramework framework);

    // ── Schedule update ───────────────────────────────────────────

    @Modifying
    @Query("""
            UPDATE Policy p
            SET p.lastEvaluatedAt  = :lastEval,
                p.nextEvaluatedAt  = :nextEval
            WHERE p.id = :policyId
            """)
    void updateEvaluationTimestamps(
            @Param("policyId") UUID policyId,
            @Param("lastEval") OffsetDateTime lastEval,
            @Param("nextEval") OffsetDateTime nextEval);

    // ── Statistics ────────────────────────────────────────────────

    @Query("""
            SELECT p.status, COUNT(p) FROM Policy p
            WHERE p.organization.id = :orgId
              AND p.deletedAt IS NULL
            GROUP BY p.status
            """)
    List<Object[]> countByStatusForOrganization(@Param("orgId") UUID organizationId);

    @Query("""
            SELECT p.framework, COUNT(p) FROM Policy p
            WHERE p.organization.id = :orgId
              AND p.deletedAt IS NULL
              AND p.status = 'ACTIVE'
            GROUP BY p.framework
            """)
    List<Object[]> countActiveByFrameworkForOrganization(@Param("orgId") UUID organizationId);

    long countByOrganizationIdAndStatusAndDeletedAtIsNull(UUID organizationId, PolicyStatus status);

    // ── Expired policies ──────────────────────────────────────────

    @Query("""
            SELECT p FROM Policy p
            WHERE p.status = 'ACTIVE'
              AND p.deletedAt IS NULL
              AND p.expiryDate IS NOT NULL
              AND p.expiryDate < CURRENT_DATE
            """)
    List<Policy> findExpiredActivePolicies();
}
