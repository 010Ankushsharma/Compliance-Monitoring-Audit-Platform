package com.company.compliance.repository;

import com.company.compliance.domain.entity.User;
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
 * Repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ── Lookup ────────────────────────────────────────────────────

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    boolean existsByEmailIgnoreCaseAndIdNotAndDeletedAtIsNull(String email, UUID id);

    // ── Organisation-scoped queries ───────────────────────────────

    Page<User> findAllByOrganizationIdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    List<User> findAllByOrganizationIdAndDeletedAtIsNull(UUID organizationId);

    @Query("""
            SELECT u FROM User u
            WHERE u.organization.id = :orgId
              AND u.deletedAt IS NULL
              AND u.active = true
              AND (:role IS NULL OR u.role = :role)
            ORDER BY u.fullName ASC
            """)
    Page<User> findByOrganizationAndRole(
            @Param("orgId") UUID organizationId,
            @Param("role") String role,
            Pageable pageable);

    // ── Account lockout ───────────────────────────────────────────

    @Query("""
            SELECT u FROM User u
            WHERE u.lockedUntil IS NOT NULL
              AND u.lockedUntil > :now
              AND u.deletedAt IS NULL
            """)
    List<User> findLockedAccounts(@Param("now") OffsetDateTime now);

    @Modifying
    @Query("""
            UPDATE User u
            SET u.lockedUntil   = NULL,
                u.failedLogins  = 0
            WHERE u.lockedUntil <= :now
              AND u.deletedAt IS NULL
            """)
    int unlockExpiredLockouts(@Param("now") OffsetDateTime now);

    // ── Login tracking ────────────────────────────────────────────

    @Modifying
    @Query("""
            UPDATE User u
            SET u.lastLoginAt  = :loginAt,
                u.failedLogins = 0,
                u.lockedUntil  = NULL
            WHERE u.id = :userId
            """)
    void recordLogin(@Param("userId") UUID userId, @Param("loginAt") OffsetDateTime loginAt);

    @Modifying
    @Query("""
            UPDATE User u
            SET u.failedLogins = u.failedLogins + 1,
                u.lockedUntil  = CASE
                    WHEN u.failedLogins + 1 >= :maxAttempts
                    THEN :lockUntil
                    ELSE u.lockedUntil
                END
            WHERE u.id = :userId
            """)
    void recordFailedLogin(
            @Param("userId") UUID userId,
            @Param("maxAttempts") int maxAttempts,
            @Param("lockUntil") OffsetDateTime lockUntil);

    // ── MFA ───────────────────────────────────────────────────────

    @Modifying
    @Query("""
            UPDATE User u
            SET u.mfaEnabled = :enabled,
                u.mfaSecret  = :secret
            WHERE u.id = :userId
            """)
    void updateMfaSettings(
            @Param("userId") UUID userId,
            @Param("enabled") boolean enabled,
            @Param("secret") String secret);

    // ── Soft delete ───────────────────────────────────────────────

    @Modifying
    @Query("""
            UPDATE User u
            SET u.deletedAt = :now,
                u.active    = false
            WHERE u.id = :userId
            """)
    void softDelete(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    // ── Statistics ────────────────────────────────────────────────

    @Query("""
            SELECT u.role, COUNT(u) FROM User u
            WHERE u.organization.id = :orgId
              AND u.deletedAt IS NULL
            GROUP BY u.role
            """)
    List<Object[]> countByRoleForOrganization(@Param("orgId") UUID organizationId);

    long countByOrganizationIdAndDeletedAtIsNull(UUID organizationId);
}
