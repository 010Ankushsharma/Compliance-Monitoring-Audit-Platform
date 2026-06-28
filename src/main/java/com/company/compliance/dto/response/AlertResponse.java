package com.company.compliance.dto.response;

import com.company.compliance.domain.enums.Severity;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Alert response")
public class AlertResponse {

    private UUID id;
    private UUID organizationId;
    private UUID violationId;
    private Severity severity;
    private String status;
    private String title;
    private String message;
    private String source;
    private String dedupKey;
    private short escalationLevel;
    private OffsetDateTime escalatedAt;
    private UUID acknowledgedById;
    private String acknowledgedByName;
    private OffsetDateTime acknowledgedAt;
    private UUID resolvedById;
    private OffsetDateTime resolvedAt;
    private boolean notificationSent;
    private OffsetDateTime notificationSentAt;
    private String[] notificationChannels;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
