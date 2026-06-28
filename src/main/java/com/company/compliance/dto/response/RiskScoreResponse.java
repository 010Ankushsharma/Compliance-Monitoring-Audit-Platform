package com.company.compliance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Policy compliance / risk score")
public class RiskScoreResponse {

    private UUID id;
    private UUID organizationId;
    private UUID policyId;
    private String policyName;
    private String framework;

    @Schema(description = "Compliance score from 0 (non-compliant) to 100 (fully compliant)")
    private BigDecimal complianceScore;

    @Schema(description = "Total penalty deducted from score due to open violations")
    private BigDecimal violationPenalty;

    private int openViolationsCount;
    private int criticalViolations;
    private int highViolations;
    private int mediumViolations;
    private int lowViolations;

    @Schema(description = "Previous compliance score for delta computation")
    private BigDecimal previousScore;

    @Schema(description = "Score change since last evaluation", example = "+5.00")
    private BigDecimal scoreDelta;

    @Schema(description = "Score trend", example = "IMPROVING")
    private String trend;

    private OffsetDateTime lastEvaluatedAt;
    private OffsetDateTime nextEvaluationAt;
}
