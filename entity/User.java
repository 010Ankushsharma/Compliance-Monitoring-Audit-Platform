package com.company.compliance.domain.entity;

import com.company.compliance.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Platform user with RBAC role.
 *
 * <p>Maps to the {@code users} table. Credentials are managed here;
 * Spring Security loads this via {@code ComplianceUserDetailsService}.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"organization", "passwordHash", "mfaSecret"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    /**
     * RBAC role. Matches the CHECK constraint in the DDL.
     * Values: SUPER_ADMIN, COMPLIANCE_OFFICER, AUDITOR, ANALYST, API_CLIENT
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    /** TOTP secret — encrypted at the application layer before persistence. */
    @Column(name = "mfa_secret", length = 255)
    private String mfaSecret;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "failed_logins", nullable = false)
    @Builder.Default
    private short failedLogins = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // ── Lifecycle ─────────────────────────────────────────────────

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Domain helpers ────────────────────────────────────────────

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now());
    }

    public boolean isAccountNonLocked() {
        return !isLocked();
    }

    public void recordSuccessfulLogin() {
        this.lastLoginAt = OffsetDateTime.now();
        this.failedLogins = 0;
        this.lockedUntil = null;
    }

    public void recordFailedLogin(int maxAttempts, int lockoutMinutes) {
        this.failedLogins++;
        if (this.failedLogins >= maxAttempts) {
            this.lockedUntil = OffsetDateTime.now().plusMinutes(lockoutMinutes);
        }
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        this.active = false;
    }
}
