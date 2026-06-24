package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested state transition is not permitted by the domain model
 * (e.g. ARCHIVED → ACTIVE, RESOLVED → IN_REVIEW without re-opening first).
 *
 * <p>HTTP 422 Unprocessable Entity.
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/InvalidStateTransitionException.java}
 */
public class InvalidStateTransitionException extends CompliancePlatformException {

    public InvalidStateTransitionException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STATE_TRANSITION");
    }
}
