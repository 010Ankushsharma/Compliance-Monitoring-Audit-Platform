package com.company.compliance.dto.request;

import com.company.compliance.domain.enums.Severity;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class CreateViolationRequest {

    @NotNull(message = "Policy ID is required")
    private UUID policyId;

    private UUID policyRuleId;
    private UUID auditLogId;
    private UUID userId;

    @NotNull(message = "Severity is required")
    private Severity severity;

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    private String title;

    @Size(max = 5000)
    private String description;

    private Map<String, Object> evidence;
}
