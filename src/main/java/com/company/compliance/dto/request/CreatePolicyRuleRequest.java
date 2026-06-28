package com.company.compliance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "A single rule within a policy")
public class CreatePolicyRuleRequest {

    @NotBlank(message = "Rule name is required")
    @Size(max = 255)
    @Schema(example = "MFA Enforcement")
    private String name;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "Rule type is required")
    @Pattern(
        regexp = "THRESHOLD|PATTERN|PRESENCE|FREQUENCY|CUSTOM",
        message = "Invalid rule type"
    )
    @Schema(example = "PRESENCE")
    private String ruleType;

    @NotBlank(message = "Field path is required")
    @Size(max = 255)
    @Schema(description = "Data field path to evaluate", example = "user.mfa_enabled")
    private String field;

    @NotBlank(message = "Operator is required")
    @Pattern(
        regexp = "EQUALS|NOT_EQUALS|GREATER_THAN|LESS_THAN|CONTAINS|NOT_CONTAINS|MATCHES_REGEX|IN|NOT_IN",
        message = "Invalid operator"
    )
    @Schema(example = "EQUALS")
    private String operator;

    @NotBlank(message = "Value is required")
    @Schema(example = "true")
    private String value;

    @Min(value = 0, message = "Grace period cannot be negative")
    @Max(value = 365, message = "Grace period cannot exceed 365 days")
    @Schema(description = "Days before a new violation is raised after initial detection", example = "7")
    private int gracePeriodDays = 0;

    @Schema(description = "Evaluation order within the policy (lower = first)", example = "1")
    private short evaluationOrder = 0;
}
