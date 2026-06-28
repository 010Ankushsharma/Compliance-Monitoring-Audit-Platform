package com.company.compliance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "JWT authentication response")
public class AuthResponse {

    @Schema(description = "Bearer access token (JWT)")
    private String accessToken;

    @Schema(description = "Refresh token for obtaining a new access token")
    private String refreshToken;

    @Schema(description = "Token type — always 'Bearer'", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token expiry time in seconds", example = "3600")
    private long expiresIn;

    @Schema(description = "Timestamp when the access token expires")
    private OffsetDateTime expiresAt;

    @Schema(description = "Authenticated user ID")
    private UUID userId;

    @Schema(description = "Authenticated user email")
    private String email;

    @Schema(description = "User's display name")
    private String fullName;

    @Schema(description = "User's RBAC role")
    private String role;

    @Schema(description = "Organisation the user belongs to")
    private UUID organizationId;

    @Schema(description = "True if MFA verification is still required")
    private boolean mfaRequired;
}
