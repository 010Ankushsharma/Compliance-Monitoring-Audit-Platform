package com.company.compliance.dto.response;

import com.company.compliance.domain.enums.PolicyStatus;
import com.company.compliance.domain.enums.RegulatoryFramework;
import com.company.compliance.domain.enums.Severity;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Compliance policy response")
public class PolicyResponse {

    private UUID id;
    private UUID organizationId;
    private String name;
    private String description;
    private RegulatoryFramework framework;
    private Severity severity;
    private PolicyStatus status;
    private int version;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private UUID ownerId;
    private String ownerName;
    private List<String> tags;
    private String evaluationSchedule;
    private OffsetDateTime lastEvaluatedAt;
    private OffsetDateTime nextEvaluatedAt;
    private UUID createdById;
    private String createdByName;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<PolicyRuleResponse> rules;
    private int ruleCount;
    private boolean evaluable;
    private boolean expired;
}
