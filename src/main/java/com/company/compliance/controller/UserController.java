package com.company.compliance.controller;

import com.company.compliance.dto.common.ApiResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.dto.request.ChangePasswordRequest;
import com.company.compliance.dto.request.CreateUserRequest;
import com.company.compliance.dto.request.UpdateUserRequest;
import com.company.compliance.dto.response.UserResponse;
import com.company.compliance.security.CompliancePrincipal;
import com.company.compliance.security.CurrentUser;
import com.company.compliance.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User management REST controller.
 *
 * <p>RBAC summary:
 * <ul>
 *   <li>SUPER_ADMIN — full access, any organisation</li>
 *   <li>COMPLIANCE_OFFICER — create/update/deactivate in own org</li>
 *   <li>All roles — read and update own profile, change own password</li>
 * </ul>
 *
 * <p>File: {@code src/main/java/com/company/compliance/controller/UserController.java}
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User account management, profiles, password, and MFA")
public class UserController {

    private final UserService userService;

    // ── POST /api/v1/users ────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER')")
    @Operation(
        summary     = "Create a new user",
        description = "COMPLIANCE_OFFICER can create users only within their own organisation. "
                    + "SUPER_ADMIN can create users in any organisation. "
                    + "Password must be ≥12 chars with upper, lower, digit and special character."
    )
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @CurrentUser CompliancePrincipal principal) {

        UserResponse user = userService.createUser(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", user));
    }

    // ── GET /api/v1/users/me ──────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the authenticated user's own profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(
                ApiResponse.success(userService.getMyProfile(principal)));
    }

    // ── GET /api/v1/users/{id} ────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Get a user by ID",
        description = "Users can always read their own profile. "
                    + "COMPLIANCE_OFFICER and above can read profiles within their organisation."
    )
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(
                ApiResponse.success(userService.getUser(id, principal)));
    }

    // ── GET /api/v1/users ─────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR')")
    @Operation(
        summary     = "List users in an organisation",
        description = "Defaults to the authenticated user's organisation. "
                    + "SUPER_ADMIN can pass any `organizationId`."
    )
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam(required = false)
            @Parameter(description = "Organisation ID (SUPER_ADMIN only for other orgs)")
            UUID organizationId,

            @RequestParam(required = false)
            @Parameter(description = "Filter by role", example = "AUDITOR")
            String role,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,

            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                userService.listUsers(organizationId, role, page, size, principal)));
    }

    // ── PUT /api/v1/users/{id} ────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Update user profile",
        description = "All fields are optional. Users can update their own `fullName` only. "
                    + "COMPLIANCE_OFFICER can update `role` and `active` for users in their org. "
                    + "SUPER_ADMIN can update any field for any user."
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success("User updated",
                userService.updateUser(id, request, principal)));
    }

    // ── POST /api/v1/users/me/change-password ────────────────────

    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Change the authenticated user's own password",
        description = "Requires the current password for verification. "
                    + "All active sessions are revoked immediately after the change."
    )
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUser CompliancePrincipal principal) {

        userService.changePassword(principal.getUserId(), request, principal);
        return ResponseEntity.ok(
                ApiResponse.success("Password changed. Please log in again."));
    }

    // ── POST /api/v1/users/{id}/change-password ───────────────────

    @PostMapping("/{id}/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Change password for a user by ID",
        description = "A user can change their own password (current password required). "
                    + "SUPER_ADMIN can change any user's password without the current password."
    )
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUser CompliancePrincipal principal) {

        userService.changePassword(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success("Password changed. All sessions revoked."));
    }

    // ── POST /api/v1/users/{id}/reset-password ────────────────────

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary     = "Admin password reset (SUPER_ADMIN only)",
        description = "Sets a new password for any user without knowing the current one. "
                    + "All existing sessions for that user are revoked immediately."
    )
    public ResponseEntity<ApiResponse<Void>> adminResetPassword(
            @PathVariable UUID id,
            @RequestBody AdminPasswordResetRequest request,
            @CurrentUser CompliancePrincipal principal) {

        userService.adminResetPassword(id, request.newPassword(), principal);
        return ResponseEntity.ok(ApiResponse.success("Password reset. User must log in again."));
    }

    // ── POST /api/v1/users/{id}/deactivate ───────────────────────

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER')")
    @Operation(
        summary     = "Deactivate a user account",
        description = "Immediately revokes all sessions and prevents further login. "
                    + "Use `activate` to re-enable the account."
    )
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success("User deactivated",
                userService.deactivateUser(id, principal)));
    }

    // ── POST /api/v1/users/{id}/activate ─────────────────────────

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Activate a deactivated user account and clear any lockout")
    public ResponseEntity<ApiResponse<UserResponse>> activate(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success("User activated",
                userService.activateUser(id, principal)));
    }

    // ── POST /api/v1/users/{id}/unlock ───────────────────────────

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER')")
    @Operation(
        summary     = "Unlock a locked user account",
        description = "Clears the failed-login counter and lockout timestamp. "
                    + "Use when a user is legitimately locked out."
    )
    public ResponseEntity<ApiResponse<UserResponse>> unlock(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success("Account unlocked",
                userService.unlockUser(id, principal)));
    }

    // ── DELETE /api/v1/users/{id} ─────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
        summary     = "Soft-delete a user (SUPER_ADMIN only)",
        description = "Marks the user as deleted and revokes all sessions. "
                    + "Deleted users cannot log in. This action is irreversible via the API."
    )
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        userService.deleteUser(id, principal);
        return ResponseEntity.ok(ApiResponse.success("User deleted"));
    }

    // ── POST /api/v1/users/{id}/mfa/disable ──────────────────────

    @PostMapping("/{id}/mfa/disable")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary     = "Disable MFA for a user",
        description = "Only the account owner or SUPER_ADMIN can disable MFA. "
                    + "Disabling MFA is an audited action."
    )
    public ResponseEntity<ApiResponse<UserResponse>> disableMfa(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success("MFA disabled",
                userService.disableMfa(id, principal)));
    }

    // ── Inline record for admin reset request ─────────────────────

    /**
     * Minimal request body for admin password reset.
     * Separate from {@link ChangePasswordRequest} — no current password required.
     */
    record AdminPasswordResetRequest(
            @NotBlank(message = "New password is required")
            @Size(min = 12, max = 128)
            @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$",
                message = "Password must contain uppercase, lowercase, digit and special character"
            )
            String newPassword
    ) {}
}
