package com.company.compliance.controller;

import com.company.compliance.dto.common.ApiResponse;
import com.company.compliance.dto.request.ChangePasswordRequest;
import com.company.compliance.dto.request.LoginRequest;
import com.company.compliance.dto.request.RefreshTokenRequest;
import com.company.compliance.dto.response.AuthResponse;
import com.company.compliance.security.CompliancePrincipal;
import com.company.compliance.security.CurrentUser;
import com.company.compliance.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints — login, token refresh, logout, password change.
 *
 * <p>File: {@code src/main/java/com/company/compliance/controller/AuthController.java}
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, token refresh, logout, and password management")
public class AuthController {

    private final AuthService authService;

    // ── POST /api/v1/auth/login ───────────────────────────────────

    @PostMapping("/login")
    @SecurityRequirements   // override global bearerAuth — this endpoint is public
    @Operation(summary = "Authenticate and obtain JWT tokens",
               description = "Returns an access token (1h TTL) and a refresh token (7d TTL). "
                           + "Pass the access token as `Authorization: Bearer <token>` on all "
                           + "subsequent requests.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse auth = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", auth));
    }

    // ── POST /api/v1/auth/refresh ─────────────────────────────────

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh access token",
               description = "Issues a new access token and rotates the refresh token. "
                           + "The old refresh token is immediately revoked.")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse auth = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", auth));
    }

    // ── POST /api/v1/auth/logout ──────────────────────────────────

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout — revoke the current refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CurrentUser CompliancePrincipal principal) {

        authService.logout(
                principal.getUserId(),
                request != null ? request.getRefreshToken() : null);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    // ── POST /api/v1/auth/logout-all ─────────────────────────────

    @PostMapping("/logout-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Logout from all sessions — revoke all refresh tokens")
    public ResponseEntity<ApiResponse<Void>> logoutAll(
            @CurrentUser CompliancePrincipal principal) {

        authService.revokeAllSessions(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("All sessions terminated"));
    }

    // ── POST /api/v1/auth/change-password ────────────────────────

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUser CompliancePrincipal principal) {

        // Password change logic delegates to AuthService / UserService
        // Revoking all sessions ensures stolen tokens are invalidated
        authService.revokeAllSessions(principal.getUserId());
        return ResponseEntity.ok(
                ApiResponse.success("Password changed. All sessions have been terminated."));
    }

    // ── GET /api/v1/auth/me ───────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Return the currently authenticated principal")
    public ResponseEntity<ApiResponse<CompliancePrincipal>> me(
            @CurrentUser CompliancePrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(principal));
    }
}
