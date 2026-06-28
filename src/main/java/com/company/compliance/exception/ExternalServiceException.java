package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a downstream dependency (Kafka, SMTP, Slack webhook) is unavailable
 * or returns an error that should be surfaced to the caller.
 *
 * <p>HTTP 503 Service Unavailable.
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/ExternalServiceException.java}
 */
public class ExternalServiceException extends CompliancePlatformException {

    public ExternalServiceException(String service, String message) {
        super("External service [" + service + "] error: " + message,
                HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_ERROR");
    }

    public ExternalServiceException(String service, String message, Throwable cause) {
        super("External service [" + service + "] error: " + message,
                HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_ERROR", cause);
    }
}
