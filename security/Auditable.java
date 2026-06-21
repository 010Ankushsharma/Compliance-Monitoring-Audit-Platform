package com.company.compliance.annotation;

import java.lang.annotation.*;

/**
 * Marks a method for automatic audit logging by {@link com.company.compliance.security.AuditAspect}.
 *
 * <p>Usage:
 * <pre>
 *   &#64;Auditable(action = "POLICY_CREATED", resourceType = "POLICY", resourceIdArg = "id")
 *   public PolicyResponse createPolicy(UUID id, CreatePolicyRequest req) { ... }
 * </pre>
 *
 * @see com.company.compliance.security.AuditAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * Action code recorded in the audit log.
     * Convention: RESOURCE_VERB, e.g. {@code POLICY_CREATED}, {@code DATA_EXPORTED}.
     */
    String action();

    /**
     * The type of resource being acted upon.
     * Examples: {@code POLICY}, {@code USER}, {@code AUDIT_LOG}, {@code REPORT}.
     */
    String resourceType() default "";

    /**
     * Name of the method parameter that holds the resource ID to log.
     * Leave blank if the resource ID is not available as a parameter.
     */
    String resourceIdArg() default "";

    /**
     * When {@code true}, the number of method arguments is included
     * in the audit details map. Defaults to {@code false} to avoid
     * logging sensitive parameter values.
     */
    boolean includeArgs() default false;
}
