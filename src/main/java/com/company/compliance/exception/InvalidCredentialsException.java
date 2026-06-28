package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when login credentials are invalid (wrong email/password).
 *
 * <p>HTTP 401 Unauthorized — intentionally generic message to prevent
 * user enumeration ("Invalid email or password" rather than "User not found").
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/InvalidCredentialsException.java}
 */
public class InvalidCredentialsException extends CompliancePlatformException {

    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
    }
}
