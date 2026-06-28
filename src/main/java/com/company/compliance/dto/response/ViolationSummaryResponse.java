package com.company.compliance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated violation summary for dashboard display")
public class ViolationSummaryResponse {

    private UUID organizationId;
    private long totalOpen;
    private long totalCritical;
    private long totalHigh;
    private long totalMedium;
    private long totalLow;
    private long totalResolved;
    private long totalFalsePositives;
    private Map<String, Long> byFramework;
    private Map<String, Long> byPolicy;
    private OffsetDateTime lastDetectedAt;
    private double averageResolutionHours;
}
