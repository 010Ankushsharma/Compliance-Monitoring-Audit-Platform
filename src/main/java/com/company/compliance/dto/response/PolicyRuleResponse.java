package com.company.compliance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class PolicyRuleResponse {
    private UUID id;
    private UUID policyId;
    private String name;
    private String description;
    private String ruleType;
    private String field;
    private String operator;
    private String value;
    private int gracePeriodDays;
    private boolean active;
    private short evaluationOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
