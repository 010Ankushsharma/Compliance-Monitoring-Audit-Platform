package com.company.compliance.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class DashboardSummaryResponse {
    private final BigDecimal overallComplianceScore;
    private final int totalPolicies;
    private final int activePolicies;
    private final long totalOpenViolations;
    private final long criticalViolations;
    private final long highViolations;
    private final long openAlerts;
    private final Map<String, BigDecimal> scoreByFramework;
    private final List<ViolationResponse> recentViolations;
    private final List<AlertResponse> recentAlerts;
    private final OffsetDateTime asOf;
}
