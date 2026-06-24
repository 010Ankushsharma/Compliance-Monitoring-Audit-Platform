package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all Compliance Platform domain errors.
 *
 * <p>Carries an HTTP status and an error code string so the
 * {@link GlobalExceptionHandler} can build consistent {@code ErrorResponse} bodies
 * without a giant if-else chain.
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/CompliancePlatformException.java}
 */
public abstract class CompliancePlatformException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String     errorCode;

    protected CompliancePlatformException(String message,
                                          HttpStatus httpStatus,
                                          String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode  = errorCode;
    }

    protected CompliancePlatformException(String message,
                                          HttpStatus httpStatus,
                                          String errorCode,
                                          Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode  = errorCode;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String     getErrorCode()  { return errorCode;  }
}
