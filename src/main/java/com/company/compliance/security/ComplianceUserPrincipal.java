package com.company.compliance.security;

import com.company.compliance.domain.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security {@link UserDetails} implementation wrapping a {@link User} entity.
 *
 * <p>Roles are stored with the {@code ROLE_} prefix that Spring Security expects.
 * A user has exactly one role (no multi-role support — RBAC is simple here).
 */
@Getter
public class ComplianceUserPrincipal implements UserDetails {

    private final UUID   userId;
    private final UUID   organizationId;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final String role;
    private final boolean active;
    private final boolean locked;
    private final Collection<? extends GrantedAuthority> authorities;

    private ComplianceUserPrincipal(User user) {
        this.userId         = user.getId();
        this.organizationId = user.getOrganization().getId();
        this.email          = user.getEmail();
        this.fullName       = user.getFullName();
        this.passwordHash   = user.getPasswordHash();
        this.role           = user.getRole();
        this.active         = user.isActive();
        this.locked         = user.isLocked();
        this.authorities    = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    public static ComplianceUserPrincipal of(User user) {
        return new ComplianceUserPrincipal(user);
    }

    // ── UserDetails ───────────────────────────────────────────────

    @Override public String getUsername()              { return email; }
    @Override public String getPassword()              { return passwordHash; }
    @Override public boolean isEnabled()               { return active; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isAccountNonLocked()      { return !locked; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
