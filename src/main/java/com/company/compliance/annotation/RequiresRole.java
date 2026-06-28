package com.company.compliance.annotation;

import java.lang.annotation.*;

/**
 * Declarative role-based access control annotation.
 *
 * <p>Combines with Spring Security's {@code @PreAuthorize} for readable
 * permission declarations on controller methods. Enforced by Spring's
 * method security proxy.
 *
 * <p>Example usage:
 * <pre>
 *   &#64;RequiresRole({"SUPER_ADMIN", "COMPLIANCE_OFFICER"})
 *   public void createPolicy(...) { ... }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {

    /**
     * One or more role names the caller must have (OR logic).
     * Role names are matched without the {@code ROLE_} prefix.
     */
    String[] value();
}
