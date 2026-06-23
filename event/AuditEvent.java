package com.company.compliance.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka event payload published to {@code compliance.audit-events}.
 *
 * <p>File: {@code src/main/java/com/company/compliance/event/AuditEvent.java}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {
    private UUID                 organizationId;
    private UUID                 userId;
    private String               userEmail;
    private String               action;
    private String               resourceType;
    private String               resourceId;
    private String               httpMethod;
    private String               endpoint;
    private String               ipAddress;
    private String               userAgent;
    private String               requestId;
    private String               outcome;
    private short                statusCode;
    private int                  durationMs;
    private Map<String, Object>  details;
    private OffsetDateTime       timestamp;
}
