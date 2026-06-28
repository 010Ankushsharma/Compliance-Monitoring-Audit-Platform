package com.company.compliance.event;

import com.company.compliance.domain.enums.Severity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka event payload published to {@code compliance.violations}.
 *
 * <p>Published when a new violation is detected by the policy evaluation engine.
 * The alert consumer subscribes to this topic and creates alerts.
 *
 * <p>File: {@code src/main/java/com/company/compliance/event/ViolationEvent.java}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ViolationEvent {
    private UUID                 violationId;
    private UUID                 organizationId;
    private UUID                 policyId;
    private String               policyName;
    private String               framework;
    private UUID                 policyRuleId;
    private UUID                 userId;
    private String               userEmail;
    private Severity             severity;
    private String               title;
    private String               description;
    private Map<String, Object>  evidence;
    private OffsetDateTime       detectedAt;
    private boolean              isNew;        // true = first detection, false = recurrence
}
