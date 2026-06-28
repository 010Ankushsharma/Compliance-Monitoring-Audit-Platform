package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user account is temporarily locked due to repeated failed logins.
 *
 * <p>HTTP 401 Unauthorized.
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/AccountLockedException.java}
 */
public class AccountLockedException extends CompliancePlatformException {

    public AccountLockedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED");
    }
}
