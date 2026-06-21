package com.company.compliance.security;

import com.company.compliance.domain.entity.User;
import com.company.compliance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Security {@link UserDetailsService} implementation.
 *
 * <p>Loads a {@link User} by email for password-based authentication
 * (used only during the initial login flow — subsequent requests are
 * authenticated via JWT without hitting the database).
 *
 * <p>Account status checks (locked, disabled, deleted) are enforced here
 * so Spring Security can return meaningful 401/403 responses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by email address.
     *
     * @param email the email address (used as the Spring Security "username")
     * @return a populated {@link UserDetails} wrapping the {@link User} entity
     * @throws UsernameNotFoundException if no active user exists with that email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .orElseThrow(() -> {
                    log.warn("Authentication attempt for unknown email: {}", email);
                    return new UsernameNotFoundException(
                            "No account found with email: " + email);
                });

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())))
                .accountExpired(false)
                .accountLocked(user.isLocked())
                .credentialsExpired(false)
                .disabled(!user.isActive())
                .build();
    }
}
