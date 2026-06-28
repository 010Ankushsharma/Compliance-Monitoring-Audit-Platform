package com.company.compliance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Update an existing user (all fields optional)")
public class UpdateUserRequest {

    @Size(min = 2, max = 255)
    @Schema(example = "Jane M. Doe")
    private String fullName;

    @Pattern(
        regexp = "SUPER_ADMIN|COMPLIANCE_OFFICER|AUDITOR|ANALYST|API_CLIENT",
        message = "Invalid role value"
    )
    @Schema(example = "COMPLIANCE_OFFICER")
    private String role;

    @Schema(description = "Enable or disable the account")
    private Boolean active;
}
