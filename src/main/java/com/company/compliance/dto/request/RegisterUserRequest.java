package com.company.compliance.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 128, message = "Password must be 12-128 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#]).+$",
        message = "Password must contain uppercase, lowercase, digit, and special character"
    )
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "COMPLIANCE_OFFICER|AUDITOR|ANALYST|API_CLIENT",
             message = "Invalid role. Allowed: COMPLIANCE_OFFICER, AUDITOR, ANALYST, API_CLIENT")
    private String role;
}
