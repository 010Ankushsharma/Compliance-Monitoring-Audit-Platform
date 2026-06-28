package com.company.compliance.security;

import com.company.compliance.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiter using Bucket4j.
 *
 * <p>Each unique IP address gets its own bucket with:
 * <ul>
 *   <li>A sustained rate of {@code app.rate-limit.requests-per-minute} (refilled per minute)</li>
 *   <li>A burst capacity of {@code app.rate-limit.burst-capacity}</li>
 * </ul>
 *
 * <p>When the bucket is exhausted the filter returns {@code 429 Too Many Requests}
 * with a {@code Retry-After} header.
 *
 * <p>Note: In a multi-node deployment, replace the in-memory {@code ConcurrentHashMap}
 * with a {@code ProxyManager} backed by Redis (Bucket4j Redis / Caffeine proxy).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final AppProperties  appProperties;
    private final ObjectMapper   objectMapper;

    /** Per-IP bucket cache. Replace with Redis-backed proxy for multi-node. */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {

        if (!appProperties.getRateLimit().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        Bucket bucket   = buckets.computeIfAbsent(clientIp, this::newBucket);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            log.warn("Rate limit exceeded for IP: {} on path: {}",
                    clientIp, request.getRequestURI());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.addHeader("X-Rate-Limit-Remaining", "0");

            objectMapper.writeValue(response.getWriter(), Map.of(
                    "status",  429,
                    "error",   "RATE_LIMIT_EXCEEDED",
                    "message", "Too many requests. Retry after " + retryAfterSeconds + " seconds.",
                    "path",    request.getRequestURI()
            ));
        }
    }

    private Bucket newBucket(String ip) {
        AppProperties.RateLimitProperties cfg = appProperties.getRateLimit();
        Bandwidth limit = Bandwidth.builder()
                .capacity(cfg.getBurstCapacity())
                .refillGreedy(cfg.getRequestsPerMinute(), Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip rate limiting for actuator and health checks
        String path = request.getServletPath();
        return path.startsWith("/actuator/");
    }
}
