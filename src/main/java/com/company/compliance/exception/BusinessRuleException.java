package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule is violated that isn't covered by a more
 * specific exception (e.g. attempting to activate an expired policy).
 *
 * <p>Maps to HTTP 400 Bad Request.
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/BusinessRuleException.java}
 */
public class BusinessRuleException extends CompliancePlatformException {

    public BusinessRuleException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION");
    }
}
