package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a JWT or refresh token is invalid, expired, or revoked.
 *
 * <p>HTTP 401 Unauthorized.
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/InvalidTokenException.java}
 */
public class InvalidTokenException extends CompliancePlatformException {

    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
    }
}
