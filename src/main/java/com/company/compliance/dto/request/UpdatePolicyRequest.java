package com.company.compliance.dto.request;

import com.company.compliance.domain.enums.PolicyStatus;
import com.company.compliance.domain.enums.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Update an existing policy (all fields optional)")
public class UpdatePolicyRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 5000)
    private String description;

    private Severity severity;
    private PolicyStatus status;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private UUID ownerId;
    private String evaluationSchedule;
    private List<String> tags;
}
