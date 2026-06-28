package com.company.compliance.security;

import com.company.compliance.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT access-token provider using JJWT 0.12.x.
 *
 * <p>Tokens are signed with HS512 (HMAC-SHA-512). The secret key is loaded
 * from {@code app.jwt.secret} and must be at least 512 bits (64 chars) in
 * production. The application will fail to start if the key is too short.
 *
 * <p>Claims embedded in every access token:
 * <ul>
 *   <li>{@code sub}    — user UUID</li>
 *   <li>{@code email}  — user email</li>
 *   <li>{@code role}   — RBAC role string</li>
 *   <li>{@code orgId}  — organisation UUID</li>
 *   <li>{@code jti}    — unique token ID (for future revocation)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    // ── Key ───────────────────────────────────────────────────────

    /**
     * Derives a {@link SecretKey} from the configured Base64-encoded secret.
     * Called lazily — cached by Spring as a singleton bean.
     */
    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(appProperties.getJwt().getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token generation ──────────────────────────────────────────

    /**
     * Generates a signed JWT access token for the given user context.
     *
     * @param userId         the user's UUID
     * @param email          the user's email address
     * @param role           the user's RBAC role
     * @param organizationId the user's organisation UUID
     * @return signed compact JWT string
     */
    public String generateAccessToken(UUID userId,
                                      String email,
                                      String role,
                                      UUID organizationId) {
        Instant now    = Instant.now();
        Instant expiry = now.plusMillis(appProperties.getJwt().getExpirationMs());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())           // jti — unique token ID
                .subject(userId.toString())                 // sub
                .issuer(appProperties.getJwt().getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claims(Map.of(
                        "email",  email,
                        "role",   role,
                        "orgId",  organizationId.toString()
                ))
                .signWith(signingKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Generates an opaque refresh token (random UUID).
     *
     * <p>The raw value is returned to the client. A SHA-256 hash of it is
     * stored in the database so the raw token never persists at rest.
     *
     * @return raw refresh token string (UUID format)
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // ── Token validation ──────────────────────────────────────────

    /**
     * Validates the token signature, expiry, and issuer.
     *
     * @param token compact JWT string
     * @return {@code true} if the token is valid
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed: {}", e.getMessage());
        } catch (SignatureException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    // ── Claims extraction ─────────────────────────────────────────

    /**
     * Extracts all claims from a valid token.
     *
     * @param token compact JWT string
     * @return parsed {@link Claims}
     * @throws JwtException if the token is invalid or expired
     */
    public Claims getClaims(String token) {
        return parseClaims(token);
    }

    public UUID getUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public UUID getOrganizationId(String token) {
        return UUID.fromString(getClaims(token).get("orgId", String.class));
    }

    public String getJti(String token) {
        return getClaims(token).getId();
    }

    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public boolean isTokenExpired(String token) {
        try {
            return getExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // ── Private helpers ───────────────────────────────────────────

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .requireIssuer(appProperties.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
