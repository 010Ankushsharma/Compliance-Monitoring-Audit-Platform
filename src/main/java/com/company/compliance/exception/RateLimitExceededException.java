package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown programmatically if rate limiting is enforced at the service layer
 * (in addition to the filter-level {@link com.company.compliance.security.RateLimitFilter}).
 *
 * <p>HTTP 429 Too Many Requests.
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/RateLimitExceededException.java}
 */
public class RateLimitExceededException extends CompliancePlatformException {

    public RateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED");
    }
}
