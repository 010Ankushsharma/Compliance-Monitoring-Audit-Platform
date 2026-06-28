package com.company.compliance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Create a new platform user")
public class CreateUserRequest {

    @NotNull(message = "Organisation ID is required")
    @Schema(description = "Target organisation", example = "a0000000-0000-0000-0000-000000000001")
    private UUID organizationId;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(example = "jane.doe@company.com")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    @Schema(example = "Jane Doe")
    private String fullName;

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    @Schema(description = "Must be ≥12 chars with upper, lower, digit and special character")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "SUPER_ADMIN|COMPLIANCE_OFFICER|AUDITOR|ANALYST|API_CLIENT",
        message = "Role must be one of: SUPER_ADMIN, COMPLIANCE_OFFICER, AUDITOR, ANALYST, API_CLIENT"
    )
    @Schema(example = "AUDITOR")
    private String role;
}
