package com.company.compliance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Audit log search / filter parameters")
public class AuditLogSearchRequest {

    @Schema(description = "Filter by user ID")
    private UUID userId;

    @Schema(description = "Filter by action code(s)", example = "[\"LOGIN\", \"DATA_ACCESS\"]")
    private List<String> actions;

    @Schema(description = "Filter by resource type", example = "CUSTOMER_RECORD")
    private String resourceType;

    @Schema(description = "Filter by resource ID", example = "cust-12345")
    private String resourceId;

    @Schema(description = "Filter by outcome", example = "FAILURE")
    private String outcome;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Start of time range (ISO-8601)", example = "2024-01-01T00:00:00Z")
    private OffsetDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "End of time range (ISO-8601)", example = "2024-12-31T23:59:59Z")
    private OffsetDateTime to;

    @Schema(description = "Filter by request correlation ID")
    private String requestId;

    @Schema(description = "Filter by IP address", example = "192.168.1.1")
    private String ipAddress;

    @Min(0) @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    private int page = 0;

    @Min(1) @Max(200) @Schema(description = "Page size", example = "50", defaultValue = "50")
    private int size = 50;

    @Schema(description = "Sort field", example = "timestamp", defaultValue = "timestamp")
    private String sortBy = "timestamp";

    @Schema(description = "Sort direction: ASC or DESC", example = "DESC", defaultValue = "DESC")
    private String sortDir = "DESC";
}
