package com.company.compliance.security;

import com.company.compliance.exception.UnauthorizedException;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Static utility methods for accessing the current authenticated principal
 * from anywhere in the application (services, aspects, etc.).
 *
 * <p>All methods throw {@link UnauthorizedException} if there is no authenticated
 * principal in the current thread's {@link SecurityContextHolder}.
 */
@UtilityClass
public class SecurityUtils {

    /** Returns the current authenticated {@link ComplianceUserPrincipal}. */
    public static ComplianceUserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof ComplianceUserPrincipal principal)) {
            throw new UnauthorizedException("No authenticated user in current security context");
        }
        return principal;
    }

    /** Returns the current user's UUID. */
    public static UUID getCurrentUserId() {
        return getCurrentPrincipal().getUserId();
    }

    /** Returns the current user's email. */
    public static String getCurrentUserEmail() {
        return getCurrentPrincipal().getEmail();
    }

    /** Returns the current user's organisation UUID. */
    public static UUID getCurrentOrganizationId() {
        return getCurrentPrincipal().getOrganizationId();
    }

    /** Returns the current user's RBAC role string (without ROLE_ prefix). */
    public static String getCurrentUserRole() {
        return getCurrentPrincipal().getRole();
    }

    /** Returns {@code true} if the current user has the given role. */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /** Returns {@code true} if the current user is a SUPER_ADMIN. */
    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    /**
     * Asserts that the current user belongs to the given organisation,
     * or is a SUPER_ADMIN (who can access all organisations).
     *
     * @throws UnauthorizedException if the user is not in the org and not a super-admin
     */
    public static void assertOrganizationAccess(UUID organizationId) {
        if (!isSuperAdmin() && !getCurrentOrganizationId().equals(organizationId)) {
            throw new UnauthorizedException(
                    "Access denied: you do not belong to organisation " + organizationId);
        }
    }
}
