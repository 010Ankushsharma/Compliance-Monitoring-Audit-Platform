package com.company.compliance.dto.response;

import com.company.compliance.domain.enums.Severity;
import com.company.compliance.domain.enums.ViolationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Compliance violation response")
public class ViolationResponse {

    private UUID id;
    private UUID organizationId;
    private UUID policyId;
    private String policyName;
    private String framework;
    private UUID policyRuleId;
    private String policyRuleName;
    private UUID userId;
    private String userEmail;
    private Severity severity;
    private ViolationStatus status;
    private String title;
    private String description;
    private Map<String, Object> evidence;
    private OffsetDateTime detectedAt;
    private UUID acknowledgedById;
    private String acknowledgedByName;
    private OffsetDateTime acknowledgedAt;
    private UUID resolvedById;
    private String resolvedByName;
    private OffsetDateTime resolvedAt;
    private String resolutionNote;
    private BigDecimal riskScore;
    private boolean affectsRiskScore;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
