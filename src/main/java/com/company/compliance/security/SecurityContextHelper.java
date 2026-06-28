package com.company.compliance.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Static utility for accessing the current {@link CompliancePrincipal}
 * from anywhere in the application without injecting a dependency.
 *
 * <p>Prefer {@code @CurrentUser} in controllers. Use this helper in
 * services and aspects where parameter injection is not available.
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {}

    public static Optional<CompliancePrincipal> getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof CompliancePrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public static Optional<UUID> getCurrentUserId() {
        return getCurrentPrincipal().map(CompliancePrincipal::getUserId);
    }

    public static Optional<UUID> getCurrentOrganizationId() {
        return getCurrentPrincipal().map(CompliancePrincipal::getOrganizationId);
    }

    public static Optional<String> getCurrentRole() {
        return getCurrentPrincipal().map(CompliancePrincipal::getRole);
    }

    /**
     * Returns the current principal or throws if not authenticated.
     * Use in service methods where authentication is guaranteed.
     */
    public static CompliancePrincipal requirePrincipal() {
        return getCurrentPrincipal()
                .orElseThrow(() -> new IllegalStateException(
                        "No authenticated principal found in SecurityContext"));
    }

    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String s && "anonymousUser".equals(s));
    }

    public static boolean hasRole(String role) {
        return getCurrentRole()
                .map(r -> r.equals(role))
                .orElse(false);
    }

    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }
}
