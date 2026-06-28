package com.company.compliance.repository;

import com.company.compliance.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link RefreshToken} entities.
 *
 * <p>Tokens are stored as SHA-256 hashes; the raw token never touches the DB.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(
            String tokenHash, OffsetDateTime now);

    // ── Revocation ────────────────────────────────────────────────

    @Modifying
    @Query("""
            UPDATE RefreshToken t
            SET t.revoked   = true,
                t.revokedAt = :now
            WHERE t.tokenHash = :hash
            """)
    int revokeByHash(@Param("hash") String tokenHash, @Param("now") OffsetDateTime now);

    /** Revoke all tokens for a user — called on password change or forced logout. */
    @Modifying
    @Query("""
            UPDATE RefreshToken t
            SET t.revoked   = true,
                t.revokedAt = :now
            WHERE t.user.id = :userId
              AND t.revoked = false
            """)
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    // ── Cleanup ───────────────────────────────────────────────────

    @Modifying
    @Query("""
            DELETE FROM RefreshToken t
            WHERE t.expiresAt < :cutoff
               OR t.revoked = true
            """)
    int deleteExpiredAndRevoked(@Param("cutoff") OffsetDateTime cutoff);

    long countByUserIdAndRevokedFalseAndExpiresAtAfter(UUID userId, OffsetDateTime now);
}
