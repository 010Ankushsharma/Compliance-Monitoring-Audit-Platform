package com.company.compliance.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateReportRequest {

    @NotBlank(message = "Template ID is required")
    private String templateId;

    @NotBlank(message = "Report title is required")
    @Size(max = 500)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Period start date is required")
    private LocalDate periodStart;

    @NotNull(message = "Period end date is required")
    private LocalDate periodEnd;

    @Pattern(regexp = "PDF|EXCEL|CSV|JSON", message = "Format must be PDF, EXCEL, CSV, or JSON")
    private String format = "PDF";

    private boolean includeEvidence = true;
}
