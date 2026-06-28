package com.company.compliance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login credentials")
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(example = "admin@company.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(example = "Admin@1234")
    private String password;

    @Schema(description = "TOTP code — required if MFA is enabled for this account", example = "123456")
    private String totpCode;
}
