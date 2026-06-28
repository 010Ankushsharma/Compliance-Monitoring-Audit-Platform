package com.company.compliance.dto.request;

import com.company.compliance.domain.enums.RegulatoryFramework;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request to generate a compliance report")
public class GenerateReportRequest {

    @NotBlank(message = "Template ID is required")
    @Schema(example = "gdpr-compliance")
    private String templateId;

    @NotBlank(message = "Title is required")
    @Schema(example = "GDPR Compliance Report Q4 2024")
    private String title;

    @Schema(example = "Quarterly GDPR compliance evidence for the Data Protection Officer")
    private String description;

    @NotNull(message = "Period start date is required")
    @Schema(example = "2024-10-01")
    private LocalDate periodStart;

    @NotNull(message = "Period end date is required")
    @Schema(example = "2024-12-31")
    private LocalDate periodEnd;

    @NotNull(message = "Format is required")
    @Pattern(regexp = "PDF|EXCEL|CSV|JSON", message = "Format must be PDF, EXCEL, CSV, or JSON")
    @Schema(example = "PDF")
    private String format;

    @Schema(description = "Frameworks to include in the report")
    private List<RegulatoryFramework> frameworks;

    @Schema(description = "Include detailed evidence attachments", defaultValue = "true")
    private boolean includeEvidence = true;
}
