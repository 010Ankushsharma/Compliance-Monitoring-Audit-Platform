package com.company.compliance.service;

import com.company.compliance.config.AppProperties;
import com.company.compliance.domain.entity.RefreshToken;
import com.company.compliance.domain.entity.User;
import com.company.compliance.dto.request.LoginRequest;
import com.company.compliance.dto.request.RefreshTokenRequest;
import com.company.compliance.dto.response.AuthResponse;
import com.company.compliance.exception.AccountLockedException;
import com.company.compliance.exception.InvalidCredentialsException;
import com.company.compliance.exception.InvalidTokenException;
import com.company.compliance.repository.RefreshTokenRepository;
import com.company.compliance.repository.UserRepository;
import com.company.compliance.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Authentication service — handles login, token refresh, and logout.
 *
 * <p>Security decisions:
 * <ul>
 *   <li>Refresh tokens stored as SHA-256 hashes — raw token never persists</li>
 *   <li>Failed logins increment counter; account locks after 5 failures for 30 min</li>
 *   <li>Password-change and logout revoke ALL existing refresh tokens for the user</li>
 * </ul>
 *
 * <p>File: {@code src/main/java/com/company/compliance/service/AuthService.java}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_LOGINS  = 5;
    private static final int LOCKOUT_MINUTES    = 30;

    private final AuthenticationManager   authenticationManager;
    private final JwtTokenProvider        jwtTokenProvider;
    private final UserRepository          userRepository;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final AppProperties           appProperties;

    // ── Login ─────────────────────────────────────────────────────

    /**
     * Authenticates credentials and returns a JWT access + refresh token pair.
     *
     * @param request login credentials (email, password, optional TOTP)
     * @return {@link AuthResponse} containing both tokens and user metadata
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (user.isLocked()) {
            throw new AccountLockedException(
                    "Account is temporarily locked. Please try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            handleFailedLogin(user);
            throw new InvalidCredentialsException("Invalid email or password");
        } catch (LockedException e) {
            throw new AccountLockedException("Account is locked. Contact your administrator.");
        } catch (DisabledException e) {
            throw new AccountLockedException("Account is disabled. Contact your administrator.");
        }

        // Record successful login
        userRepository.recordLogin(user.getId(), OffsetDateTime.now());

        return buildAuthResponse(user);
    }

    // ── Token Refresh ─────────────────────────────────────────────

    /**
     * Issues a new access token using a valid refresh token.
     * The old refresh token is revoked (rotation) and a new one is issued.
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = sha256(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenHashAndRevokedFalseAndExpiresAtAfter(
                        tokenHash, OffsetDateTime.now())
                .orElseThrow(() -> new InvalidTokenException(
                        "Refresh token is invalid, expired, or already revoked"));

        User user = storedToken.getUser();

        // Rotate: revoke old token, issue new one
        refreshTokenRepository.revokeByHash(tokenHash, OffsetDateTime.now());

        return buildAuthResponse(user);
    }

    // ── Logout ────────────────────────────────────────────────────

    /**
     * Revokes the provided refresh token. Passing {@code null} revokes all
     * tokens for the user (full sign-out from all sessions).
     */
    @Transactional
    public void logout(UUID userId, String refreshToken) {
        if (refreshToken != null) {
            refreshTokenRepository.revokeByHash(sha256(refreshToken), OffsetDateTime.now());
        } else {
            refreshTokenRepository.revokeAllForUser(userId, OffsetDateTime.now());
        }
        log.info("User {} logged out", userId);
    }

    /**
     * Revokes ALL refresh tokens for a user (e.g. after password change).
     */
    @Transactional
    public void revokeAllSessions(UUID userId) {
        int revoked = refreshTokenRepository.revokeAllForUser(userId, OffsetDateTime.now());
        log.info("Revoked {} refresh token(s) for user {}", revoked, userId);
    }

    // ── Private helpers ───────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        // Generate access token
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getOrganization().getId());

        // Generate and store refresh token (hash only persisted)
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken();
        saveRefreshToken(user, rawRefreshToken);

        long expiresInSeconds = appProperties.getJwt().getExpirationMs() / 1000;

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .expiresAt(OffsetDateTime.now().plusSeconds(expiresInSeconds))
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .organizationId(user.getOrganization().getId())
                .mfaRequired(user.isMfaEnabled())
                .build();
    }

    private void saveRefreshToken(User user, String rawToken) {
        long refreshExpiryMs = appProperties.getJwt().getRefreshExpirationMs();
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256(rawToken))
                .issuedAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusSeconds(refreshExpiryMs / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
    }

    private void handleFailedLogin(User user) {
        userRepository.recordFailedLogin(
                user.getId(),
                MAX_FAILED_LOGINS,
                OffsetDateTime.now().plusMinutes(LOCKOUT_MINUTES));
        log.warn("Failed login attempt for user {}. Failures: {}",
                user.getEmail(), user.getFailedLogins() + 1);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
