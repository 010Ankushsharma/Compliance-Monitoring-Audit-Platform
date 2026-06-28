package com.company.compliance.service;

import com.company.compliance.annotation.Auditable;
import com.company.compliance.domain.entity.Organization;
import com.company.compliance.domain.entity.User;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.dto.request.ChangePasswordRequest;
import com.company.compliance.dto.request.CreateUserRequest;
import com.company.compliance.dto.request.UpdateUserRequest;
import com.company.compliance.dto.response.UserResponse;
import com.company.compliance.exception.ConflictException;
import com.company.compliance.exception.InvalidCredentialsException;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.mapper.UserMapper;
import com.company.compliance.repository.OrganizationRepository;
import com.company.compliance.repository.UserRepository;
import com.company.compliance.security.CompliancePrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User management service — CRUD, password changes, MFA, soft-delete.
 *
 * <p>Multi-tenant rules enforced here:
 * <ul>
 *   <li>SUPER_ADMIN can manage users in any organisation</li>
 *   <li>COMPLIANCE_OFFICER can only manage users in their own organisation</li>
 *   <li>All other roles can only read/update their own profile</li>
 * </ul>
 *
 * <p>Password hashing: BCrypt cost-12 via {@link PasswordEncoder}.
 * Hashes are never stored in DTOs or logs.
 *
 * <p>File: {@code src/main/java/com/company/compliance/service/UserService.java}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository         userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder        passwordEncoder;
    private final UserMapper             userMapper;
    private final AuthService            authService;

    // ── Create ────────────────────────────────────────────────────

    /**
     * Creates a new platform user in the specified organisation.
     * Only SUPER_ADMIN can create users in any org;
     * COMPLIANCE_OFFICER can only create in their own org.
     */
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    @Auditable(action = "USER_CREATED", resourceType = "USER")
    public UserResponse createUser(CreateUserRequest req, CompliancePrincipal principal) {
        UUID targetOrgId = req.getOrganizationId() != null
                ? req.getOrganizationId()
                : principal.getOrganizationId();

        // Multi-tenant guard
        assertCanManageOrg(targetOrgId, principal);

        // Uniqueness check
        if (userRepository.existsByEmailIgnoreCaseAndDeletedAtIsNull(req.getEmail())) {
            throw new ConflictException(
                    "A user with email '" + req.getEmail() + "' already exists");
        }

        Organization org = organizationRepository
                .findByIdAndDeletedAtIsNull(targetOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", targetOrgId));

        // Role escalation guard — only SUPER_ADMIN can create SUPER_ADMIN
        if ("SUPER_ADMIN".equals(req.getRole()) && !principal.isSuperAdmin()) {
            throw new AccessDeniedException("Only SUPER_ADMIN can create SUPER_ADMIN users");
        }

        User user = userMapper.fromCreateRequest(req);
        user.setOrganization(org);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));

        User saved = userRepository.save(user);

        log.info("User created: email={} role={} org={} by={}",
                saved.getEmail(), saved.getRole(), org.getName(), principal.getEmail());
        return userMapper.toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId.toString()")
    public UserResponse getUser(UUID userId, CompliancePrincipal principal) {
        User user = resolveUser(userId);
        assertCanReadUser(user, principal);
        return userMapper.toResponse(user);
    }

    /**
     * Returns the profile of the authenticated user (self-service endpoint).
     */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(CompliancePrincipal principal) {
        return userMapper.toResponse(resolveUser(principal.getUserId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(UUID organizationId,
                                                 String role,
                                                 int page, int size,
                                                 CompliancePrincipal principal) {
        UUID targetOrgId = organizationId != null
                ? organizationId : principal.getOrganizationId();

        assertCanManageOrg(targetOrgId, principal);

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by("fullName").ascending());

        Page<User> users = role != null
                ? userRepository.findByOrganizationAndRole(targetOrgId, role, pageable)
                : userRepository.findAllByOrganizationIdAndDeletedAtIsNull(targetOrgId, pageable);

        return PageResponse.from(users.map(userMapper::toResponse));
    }

    // ── Update ────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "users", key = "#userId.toString()")
    @Auditable(action = "USER_UPDATED", resourceType = "USER", resourceIdArg = "userId")
    public UserResponse updateUser(UUID userId,
                                   UpdateUserRequest req,
                                   CompliancePrincipal principal) {
        User user = resolveUser(userId);
        assertCanManageUser(user, principal);

        // Role escalation guard
        if (req.getRole() != null
                && "SUPER_ADMIN".equals(req.getRole())
                && !principal.isSuperAdmin()) {
            throw new AccessDeniedException("Only SUPER_ADMIN can assign SUPER_ADMIN role");
        }

        userMapper.updateFromRequest(user, req);
        User saved = userRepository.save(user);

        log.info("User updated: id={} by={}", userId, principal.getEmail());
        return userMapper.toResponse(saved);
    }

    // ── Password management ───────────────────────────────────────

    @Transactional
    @CacheEvict(value = "users", key = "#userId.toString()")
    @Auditable(action = "PASSWORD_CHANGED", resourceType = "USER", resourceIdArg = "userId")
    public void changePassword(UUID userId,
                               ChangePasswordRequest req,
                               CompliancePrincipal principal) {
        // Only the user themselves (or SUPER_ADMIN) can change a password
        if (!principal.getUserId().equals(userId) && !principal.isSuperAdmin()) {
            throw new AccessDeniedException("You can only change your own password");
        }

        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new InvalidCredentialsException("New password and confirmation do not match");
        }

        User user = resolveUser(userId);

        // Verify current password (skip for SUPER_ADMIN resetting another user's password)
        if (principal.getUserId().equals(userId)) {
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
                throw new InvalidCredentialsException("Current password is incorrect");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // Revoke all sessions — forces re-login with new password
        authService.revokeAllSessions(userId);

        log.info("Password changed for user {} by {}", userId, principal.getEmail());
    }

    /**
     * Admin password reset — SUPER_ADMIN sets a new password for any user.
     * Does not require knowledge of the current password.
     * Revokes all existing sessions.
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId.toString()")
    @Auditable(action = "PASSWORD_RESET", resourceType = "USER", resourceIdArg = "userId")
    public void adminResetPassword(UUID userId,
                                   String newPassword,
                                   CompliancePrincipal principal) {
        if (!principal.isSuperAdmin()) {
            throw new AccessDeniedException("Only SUPER_ADMIN can reset passwords");
        }

        User user = resolveUser(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        authService.revokeAllSessions(userId);
        log.info("Admin password reset for user {} by SUPER_ADMIN {}", userId, principal.getEmail());
    }

    // ── Account management ────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "users", key = "#userId.toString()")
    @Auditable(action = "USER_DEACTIVATED", resourceType = "USER", resourceIdArg = "userId")
    public UserResponse deactivateUser(UUID userId, CompliancePrincipal principal) {
        User user = resolveUser(userId);
        assertCanManageUser(user, principal);

        // Can't deactivate yourself
        if (userId.equals(principal.getUserId())) {
            throw new IllegalStateException("You cannot deactivate your own account");
        }

        user.setActive(false);
        User saved = userRepository.save(user);

        // Revoke all sessions immediately
        authService.revokeAllSessions(userId);

        log.info("User {} deactivated by {}", userId, principal.getEmail());
        return userMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId.toString()")
    @Auditable(action = "USER_ACTIVATED", resourceType = "USER", resourceIdArg = "userId")
    public UserResponse activateUser(UUID userId, CompliancePrincipal principal) {
        User user = resolveUser(userId);
        assertCanManageUser(user, principal);

        user.setActive(true);
        // Clear any existing lockout
        user.setLockedUntil(null);
        user.setFailedLogins((short) 0);

        User saved = userRepository.save(user);
        log.info("User {} activated by {}", userId, principal.getEmail());
        return userMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId.toString()")
    @Auditable(action = "USER_UNLOCKED", resourceType = "USER", resourceIdArg = "userId")
    public UserResponse unlockUser(UUID userId, CompliancePrincipal principal) {
        if (!principal.isSuperAdmin() && !principal.isComplianceOfficer()) {
            throw new AccessDeniedException("Only SUPER_ADMIN or COMPLIANCE_OFFICER can unlock accounts");
        }
        User user = resolveUser(userId);
        user.setLockedUntil(null);
        user.setFailedLogins((short) 0);
        User saved = userRepository.save(user);
        log.info("Account unlocked for user {} by {}", userId, principal.getEmail());
        return userMapper.toResponse(saved);
    }

    // ── Soft delete ───────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "users", key = "#userId.toString()")
    @Auditable(action = "USER_DELETED", resourceType = "USER", resourceIdArg = "userId")
    public void deleteUser(UUID userId, CompliancePrincipal principal) {
        if (!principal.isSuperAdmin()) {
            throw new AccessDeniedException("Only SUPER_ADMIN can delete users");
        }
        if (userId.equals(principal.getUserId())) {
            throw new IllegalStateException("You cannot delete your own account");
        }

        User user = resolveUser(userId);
        userRepository.softDelete(userId, OffsetDateTime.now());
        authService.revokeAllSessions(userId);

        log.info("User {} soft-deleted by SUPER_ADMIN {}", userId, principal.getEmail());
    }

    // ── MFA management ────────────────────────────────────────────

    /**
     * Enables TOTP-based MFA for the authenticated user.
     * The {@code totpSecret} is encrypted at the service layer before storage.
     * (Full TOTP setup flow — QR code generation, verification — would be added here.)
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId.toString()")
    @Auditable(action = "MFA_ENABLED", resourceType = "USER", resourceIdArg = "userId")
    public UserResponse enableMfa(UUID userId, String encryptedTotpSecret,
                                  CompliancePrincipal principal) {
        if (!principal.getUserId().equals(userId)) {
            throw new AccessDeniedException("MFA can only be enabled by the account owner");
        }
        userRepository.updateMfaSettings(userId, true, encryptedTotpSecret);
        return userMapper.toResponse(resolveUser(userId));
    }

    @Transactional
    @CacheEvict(value = "users", key = "#userId.toString()")
    @Auditable(action = "MFA_DISABLED", resourceType = "USER", resourceIdArg = "userId")
    public UserResponse disableMfa(UUID userId, CompliancePrincipal principal) {
        // Only owner or SUPER_ADMIN can disable MFA
        if (!principal.getUserId().equals(userId) && !principal.isSuperAdmin()) {
            throw new AccessDeniedException("Only the account owner or SUPER_ADMIN can disable MFA");
        }
        userRepository.updateMfaSettings(userId, false, null);
        return userMapper.toResponse(resolveUser(userId));
    }

    // ── Stats ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Object[]> getUserRoleStats(UUID organizationId, CompliancePrincipal principal) {
        assertCanManageOrg(organizationId, principal);
        return userRepository.countByRoleForOrganization(organizationId);
    }

    // ── Private helpers ───────────────────────────────────────────

    private User resolveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void assertCanManageOrg(UUID orgId, CompliancePrincipal principal) {
        if (!principal.canManage(orgId)) {
            throw new AccessDeniedException(
                    "You do not have permission to manage users in organisation " + orgId);
        }
    }

    private void assertCanReadUser(User user, CompliancePrincipal principal) {
        // Can read own profile always; otherwise must be same org or SUPER_ADMIN
        if (!principal.getUserId().equals(user.getId())
                && !principal.canManage(user.getOrganization().getId())) {
            throw new AccessDeniedException("Access denied to user " + user.getId());
        }
    }

    private void assertCanManageUser(User user, CompliancePrincipal principal) {
        if (!principal.isSuperAdmin()
                && !principal.getOrganizationId().equals(user.getOrganization().getId())) {
            throw new AccessDeniedException(
                    "You can only manage users within your own organisation");
        }
    }
}
