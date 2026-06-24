package com.company.compliance.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Thrown when a requested resource does not exist or is not visible
 * to the authenticated tenant.
 *
 * <p>HTTP 404 Not Found.
 *
 * <p>File: {@code src/main/java/com/company/compliance/exception/ResourceNotFoundException.java}
 */
public class ResourceNotFoundException extends CompliancePlatformException {

    public ResourceNotFoundException(String resourceType, UUID id) {
        super(resourceType + " not found: " + id, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found: " + identifier, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
