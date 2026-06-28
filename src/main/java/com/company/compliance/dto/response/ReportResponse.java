package com.company.compliance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Compliance report metadata response")
public class ReportResponse {

    private UUID id;
    private UUID organizationId;
    private String templateId;
    private String title;
    private String description;
    private String status;
    private String format;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private boolean includeEvidence;
    private Long fileSizeBytes;
    private UUID generatedById;
    private String generatedByName;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private String errorMessage;
    private Map<String, Object> summary;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Schema(description = "Download URL (present when status = COMPLETED)")
    private String downloadUrl;
}
