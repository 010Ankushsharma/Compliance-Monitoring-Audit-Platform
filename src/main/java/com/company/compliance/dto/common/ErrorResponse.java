package com.company.compliance.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Structured error response returned by {@code GlobalExceptionHandler}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Structured error response")
public class ErrorResponse {

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error classification", example = "VALIDATION_ERROR")
    private String error;

    @Schema(description = "Human-readable error message", example = "Request validation failed")
    private String message;

    @Schema(description = "Request path that produced the error", example = "/api/v1/policies")
    private String path;

    @Schema(description = "ISO-8601 timestamp", example = "2024-01-15T10:30:00Z")
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Schema(description = "Correlation ID for support tracing", example = "req-abc-123")
    private String requestId;

    @Schema(description = "Field-level validation errors")
    private Map<String, List<String>> fieldErrors;

    @Schema(description = "Additional error details")
    private List<String> details;
}
