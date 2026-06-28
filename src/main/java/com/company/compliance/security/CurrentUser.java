package com.company.compliance.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Convenience annotation for injecting the {@link CompliancePrincipal}
 * into controller methods.
 *
 * <p>Usage:
 * <pre>
 *   &#64;GetMapping("/me")
 *   public UserResponse getMe(&#64;CurrentUser CompliancePrincipal principal) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
