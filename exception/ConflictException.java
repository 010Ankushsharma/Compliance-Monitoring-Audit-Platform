package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would create a duplicate or violate a uniqueness constraint.
 *
 * <p>HTTP 409 Conflict.
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/ConflictException.java}
 */
public class ConflictException extends CompliancePlatformException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "CONFLICT");
    }
}
