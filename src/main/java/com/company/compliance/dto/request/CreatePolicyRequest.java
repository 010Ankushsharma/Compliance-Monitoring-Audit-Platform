package com.company.compliance.dto.request;

import com.company.compliance.domain.enums.RegulatoryFramework;
import com.company.compliance.domain.enums.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Create a new compliance policy")
public class CreatePolicyRequest {

    @NotBlank(message = "Policy name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    @Schema(example = "GDPR Data Retention Policy")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Schema(example = "Ensures personal data is not retained beyond the consented period.")
    private String description;

    @NotNull(message = "Framework is required")
    @Schema(example = "GDPR")
    private RegulatoryFramework framework;

    @NotNull(message = "Severity is required")
    @Schema(example = "CRITICAL")
    private Severity severity;

    @Schema(description = "Policy effective date (ISO-8601 date)", example = "2024-01-01")
    private LocalDate effectiveDate;

    @Schema(description = "Policy expiry date (ISO-8601 date)", example = "2025-12-31")
    private LocalDate expiryDate;

    @Schema(description = "UUID of the policy owner (user)")
    private UUID ownerId;

    @Schema(description = "Cron expression for scheduled evaluation", example = "0 0 * * *")
    @Pattern(
        regexp = "^(\\*|([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])|\\*\\/([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])) (\\*|([0-9]|1[0-9]|2[0-3])|\\*\\/([0-9]|1[0-9]|2[0-3])) (\\*|([1-9]|1[0-9]|2[0-9]|3[0-1])|\\*\\/([1-9]|1[0-9]|2[0-9]|3[0-1])) (\\*|([1-9]|1[0-2])|\\*\\/([1-9]|1[0-2])) (\\*|([0-6])|\\*\\/([0-6]))$",
        message = "Must be a valid 5-field cron expression"
    )
    private String evaluationSchedule;

    @Schema(description = "Tags for filtering and grouping")
    private List<String> tags = new ArrayList<>();

    @Valid
    @Schema(description = "Initial policy rules to attach")
    private List<CreatePolicyRuleRequest> rules = new ArrayList<>();
}
