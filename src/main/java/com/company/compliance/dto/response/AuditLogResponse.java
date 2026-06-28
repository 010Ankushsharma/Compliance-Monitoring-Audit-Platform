package com.company.compliance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Immutable audit log entry response")
public class AuditLogResponse {

    private UUID id;
    private UUID organizationId;
    private OffsetDateTime timestamp;
    private UUID userId;
    private String userEmail;
    private String action;
    private String resourceType;
    private String resourceId;
    private String resourceName;
    private String httpMethod;
    private String endpoint;
    private String ipAddress;
    private String requestId;
    private String outcome;
    private Short statusCode;
    private Integer durationMs;
    private Map<String, Object> details;
    private String hash;
    private String previousHash;
    private Long sequenceNumber;
}
