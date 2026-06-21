package com.company.compliance.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter — runs once per request before Spring Security's
 * standard filter chain.
 *
 * <p>Extracts the Bearer token from the {@code Authorization} header,
 * validates it, and populates the {@link SecurityContextHolder} with a
 * {@link CompliancePrincipal} containing the user's ID, role, and org.
 *
 * <p>On any validation failure the filter simply passes the request through
 * with no authentication context — Spring Security will then enforce access
 * control and return 401 at the controller layer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";
    private static final String REQUEST_ID_HEADER    = "X-Request-ID";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        // Propagate correlation ID for distributed tracing
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestId)) {
            org.slf4j.MDC.put("requestId", requestId);
        }

        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                authenticateRequest(token, request);
            }

        } catch (Exception ex) {
            // Log but never block — Spring Security handles 401 responses
            log.debug("JWT authentication failed for request {}: {}",
                    request.getRequestURI(), ex.getMessage());
        } finally {
            org.slf4j.MDC.clear();
        }

        filterChain.doFilter(request, response);
    }

    // ── Private helpers ───────────────────────────────────────────

    private void authenticateRequest(String token, HttpServletRequest request) {
        Claims claims          = jwtTokenProvider.getClaims(token);
        UUID   userId          = UUID.fromString(claims.getSubject());
        String role            = claims.get("role",  String.class);
        String email           = claims.get("email", String.class);
        UUID   organizationId  = UUID.fromString(claims.get("orgId", String.class));

        CompliancePrincipal principal = CompliancePrincipal.builder()
                .userId(userId)
                .email(email)
                .role(role)
                .organizationId(organizationId)
                .build();

        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + role));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Enrich MDC for structured logging
        org.slf4j.MDC.put("userId", userId.toString());
        org.slf4j.MDC.put("orgId",  organizationId.toString());
        org.slf4j.MDC.put("role",   role);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT check for public endpoints
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/actuator/")
                || path.startsWith("/api-docs")
                || path.startsWith("/swagger-ui");
    }
}
