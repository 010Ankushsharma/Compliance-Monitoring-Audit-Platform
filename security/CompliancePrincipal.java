package com.company.compliance.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Authenticated principal stored in the {@link org.springframework.security.core.context.SecurityContext}.
 *
 * <p>Carries the claims decoded from the JWT so downstream code can access
 * user identity and tenant context without a DB round-trip.
 *
 * <p>Accessed via:
 * <pre>
 *   CompliancePrincipal principal =
 *       (CompliancePrincipal) SecurityContextHolder.getContext()
 *           .getAuthentication().getPrincipal();
 * </pre>
 *
 * Or via the {@code @CurrentUser} annotation helper.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompliancePrincipal implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID   userId;
    private String email;
    private String role;
    private UUID   organizationId;

    // ── Role helpers ──────────────────────────────────────────────

    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equals(role);
    }

    public boolean isComplianceOfficer() {
        return "COMPLIANCE_OFFICER".equals(role);
    }

    public boolean isAuditor() {
        return "AUDITOR".equals(role);
    }

    public boolean isAnalyst() {
        return "ANALYST".equals(role);
    }

    public boolean isApiClient() {
        return "API_CLIENT".equals(role);
    }

    /**
     * Returns {@code true} if this principal belongs to the given organisation.
     * Used for multi-tenant isolation checks in service methods.
     */
    public boolean belongsTo(UUID organizationId) {
        return this.organizationId != null
                && this.organizationId.equals(organizationId);
    }

    /**
     * Returns {@code true} if this principal can manage resources in the given org.
     * Super admins can manage any org; others are restricted to their own.
     */
    public boolean canManage(UUID targetOrganizationId) {
        return isSuperAdmin() || belongsTo(targetOrganizationId);
    }

    @Override
    public String toString() {
        return "CompliancePrincipal{userId=" + userId
                + ", role=" + role
                + ", orgId=" + organizationId + "}";
    }
}
