package com.company.compliance.event;

import com.company.compliance.domain.enums.Severity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Kafka event payload published to {@code compliance.alerts}.
 *
 * <p>File: {@code src/main/java/com/company/compliance/event/AlertEvent.java}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertEvent {
    private UUID           alertId;
    private UUID           organizationId;
    private UUID           violationId;
    private Severity       severity;
    private String         status;
    private String         title;
    private String         dedupKey;
    private OffsetDateTime createdAt;
}
