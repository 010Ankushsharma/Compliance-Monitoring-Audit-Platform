package com.company.compliance.security;

import com.company.compliance.annotation.Auditable;
import com.company.compliance.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AOP aspect that intercepts methods annotated with {@link Auditable}
 * and publishes an immutable audit log entry for each invocation.
 *
 * <p>The aspect runs {@code @Around} so it captures both the method result
 * and any thrown exception, recording the outcome ({@code SUCCESS} / {@code FAILURE}).
 *
 * <p>Audit publishing is always asynchronous — it never adds latency to the
 * request thread.
 *
 * <p>Usage on a controller or service method:
 * <pre>
 *   &#64;Auditable(action = "POLICY_CREATED", resourceType = "POLICY")
 *   public PolicyResponse createPolicy(...) { ... }
 * </pre>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private final AuditLogService auditLogService;

    @Around("@annotation(com.company.compliance.annotation.Auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startMs = System.currentTimeMillis();

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method          method          = methodSignature.getMethod();
        Auditable       annotation      = method.getAnnotation(Auditable.class);

        // ── Extract HTTP context ──────────────────────────────────
        HttpServletRequest  httpRequest  = null;
        HttpServletResponse httpResponse = null;

        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            httpRequest  = attrs.getRequest();
            httpResponse = attrs.getResponse();
        }

        // ── Extract authenticated principal ───────────────────────
        CompliancePrincipal principal = extractPrincipal();

        // ── Resolve resource ID from method arguments ─────────────
        String resourceId = resolveResourceId(joinPoint, methodSignature, annotation);

        // ── Proceed and capture outcome ───────────────────────────
        String  outcome     = "SUCCESS";
        short   statusCode  = 200;
        Throwable thrown    = null;

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable t) {
            outcome = "FAILURE";
            thrown  = t;
            result  = null;
        }

        long durationMs = System.currentTimeMillis() - startMs;

        if (httpResponse != null) {
            statusCode = (short) httpResponse.getStatus();
        }

        // ── Build detail map ──────────────────────────────────────
        Map<String, Object> details = new HashMap<>();
        details.put("method", method.getName());
        details.put("class",  joinPoint.getTarget().getClass().getSimpleName());
        if (annotation.includeArgs() && joinPoint.getArgs().length > 0) {
            details.put("argCount", joinPoint.getArgs().length);
        }

        // ── Publish audit event (async — fire and forget) ─────────
        try {
            auditLogService.publishAuditEvent(
                    principal  != null ? principal.getOrganizationId() : null,
                    principal  != null ? principal.getUserId()         : null,
                    principal  != null ? principal.getEmail()          : "SYSTEM",
                    annotation.action(),
                    annotation.resourceType(),
                    resourceId,
                    httpRequest  != null ? httpRequest.getMethod()                        : null,
                    httpRequest  != null ? httpRequest.getRequestURI()                    : null,
                    httpRequest  != null ? extractClientIp(httpRequest)                   : null,
                    httpRequest  != null ? httpRequest.getHeader("User-Agent")            : null,
                    httpRequest  != null ? httpRequest.getHeader(REQUEST_ID_HEADER)       : null,
                    outcome,
                    statusCode,
                    (int) durationMs,
                    details
            );
        } catch (Exception e) {
            // Audit failure must never affect the primary request
            log.error("Failed to publish audit event for action [{}]: {}",
                    annotation.action(), e.getMessage(), e);
        }

        if (thrown != null) {
            throw thrown;
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────

    private CompliancePrincipal extractPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CompliancePrincipal p) {
            return p;
        }
        return null;
    }

    /**
     * Tries to resolve the resource ID from a method argument named
     * {@code id}, {@code policyId}, {@code userId}, etc., or falls back
     * to the annotation's {@code resourceId} if set.
     */
    private String resolveResourceId(ProceedingJoinPoint joinPoint,
                                     MethodSignature methodSignature,
                                     Auditable annotation) {
        if (!annotation.resourceIdArg().isEmpty()) {
            String[] paramNames = methodSignature.getParameterNames();
            Object[] args       = joinPoint.getArgs();
            for (int i = 0; i < paramNames.length; i++) {
                if (annotation.resourceIdArg().equals(paramNames[i]) && args[i] != null) {
                    return args[i].toString();
                }
            }
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
