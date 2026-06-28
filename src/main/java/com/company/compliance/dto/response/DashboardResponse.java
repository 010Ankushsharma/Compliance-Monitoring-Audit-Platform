package com.company.compliance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Executive compliance dashboard summary")
public class DashboardResponse {

    private UUID organizationId;
    private String organizationName;

    @Schema(description = "Overall compliance score (0–100)")
    private BigDecimal overallComplianceScore;

    @Schema(description = "Score trend: IMPROVING, DECLINING, STABLE")
    private String scoreTrend;

    // Violation summary
    private long totalOpenViolations;
    private long criticalViolations;
    private long highViolations;
    private long mediumViolations;
    private long lowViolations;

    // Policy summary
    private long totalPolicies;
    private long activePolicies;
    private long draftPolicies;

    // Alert summary
    private long openAlerts;
    private long criticalAlerts;

    // Per-framework scores
    @Schema(description = "Compliance score per regulatory framework")
    private Map<String, BigDecimal> scoreByFramework;

    // Recent activity
    private List<ViolationResponse> recentViolations;
    private List<AlertResponse> recentAlerts;

    @Schema(description = "Timestamp of the last risk score evaluation")
    private OffsetDateTime lastEvaluatedAt;

    @Schema(description = "When this dashboard snapshot was generated")
    @Builder.Default
    private OffsetDateTime generatedAt = OffsetDateTime.now();
}
