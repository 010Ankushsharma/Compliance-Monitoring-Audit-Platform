package com.company.compliance.annotation;

import java.lang.annotation.*;

/**
 * Marks a controller method as rate-limited via Bucket4j.
 *
 * <p>The {@code RateLimitingAspect} enforces the limit. If omitted,
 * the global rate limit from {@code AppProperties} applies.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimited {

    /** Maximum requests per minute. -1 = use global default. */
    int requestsPerMinute() default -1;

    /** Key strategy: IP, USER, or GLOBAL. */
    String keyStrategy() default "USER";
}
