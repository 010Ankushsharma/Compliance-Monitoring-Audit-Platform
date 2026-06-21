package com.company.compliance.repository;

import com.company.compliance.domain.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link Organization} entities.
 *
 * <p>All queries exclude soft-deleted rows ({@code deleted_at IS NULL}) unless
 * explicitly noted.
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    // ── Lookup ────────────────────────────────────────────────────

    Optional<Organization> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Organization> findByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndIdNotAndDeletedAtIsNull(String name, UUID id);

    // ── Active organisations list ─────────────────────────────────

    Page<Organization> findAllByActiveIsTrueAndDeletedAtIsNull(Pageable pageable);

    List<Organization> findAllByActiveIsTrueAndDeletedAtIsNull();

    // ── Filter by industry / framework ───────────────────────────

    @Query("""
            SELECT o FROM Organization o
            WHERE o.deletedAt IS NULL
              AND o.active = true
              AND (:industry IS NULL OR LOWER(o.industry) = LOWER(:industry))
            ORDER BY o.name ASC
            """)
    Page<Organization> findByIndustry(
            @Param("industry") String industry,
            Pageable pageable);

    /**
     * Find organisations subscribed to a given regulatory framework.
     * Uses PostgreSQL array containment operator via native SQL.
     */
    @Query(value = """
            SELECT * FROM organizations
            WHERE deleted_at IS NULL
              AND active = TRUE
              AND :framework = ANY(regulatory_frameworks)
            ORDER BY name ASC
            """, nativeQuery = true)
    List<Organization> findByRegulatoryFramework(@Param("framework") String framework);

    // ── Risk score update ────────────────────────────────────────

    @Modifying
    @Query("""
            UPDATE Organization o
            SET o.overallRiskScore = :score,
                o.riskLastUpdated  = :updatedAt
            WHERE o.id = :orgId
            """)
    void updateOverallRiskScore(
            @Param("orgId") UUID orgId,
            @Param("score") BigDecimal score,
            @Param("updatedAt") OffsetDateTime updatedAt);

    // ── Statistics ───────────────────────────────────────────────

    @Query("SELECT COUNT(o) FROM Organization o WHERE o.active = true AND o.deletedAt IS NULL")
    long countActiveOrganizations();

    @Query(value = """
            SELECT industry, COUNT(*) AS cnt
            FROM organizations
            WHERE deleted_at IS NULL AND active = TRUE AND industry IS NOT NULL
            GROUP BY industry
            ORDER BY cnt DESC
            """, nativeQuery = true)
    List<Object[]> countByIndustry();
}
