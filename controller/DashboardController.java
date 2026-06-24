package com.company.compliance.controller;

import com.company.compliance.domain.entity.Organization;
import com.company.compliance.dto.common.ApiResponse;
import com.company.compliance.dto.response.*;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.repository.*;
import com.company.compliance.security.CompliancePrincipal;
import com.company.compliance.security.CurrentUser;
import com.company.compliance.service.RiskScoringService;
import com.company.compliance.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Executive compliance dashboard REST controller.
 *
 * <p>Aggregates data from multiple services into a single fast response for
 * the main dashboard view. Results are cached for 2 minutes (see RedisConfig).
 *
 * <p>File: {@code src/main/java/com/company/compliance/controller/DashboardController.java}
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Executive compliance dashboard — aggregated KPIs and recent activity")
public class DashboardController {

    private final OrganizationRepository        organizationRepository;
    private final ComplianceViolationRepository violationRepository;
    private final AlertRepository               alertRepository;
    private final PolicyRepository              policyRepository;
    private final RiskScoringService            riskScoringService;
    private final ViolationService              violationService;

    // ── GET /api/v1/dashboard ─────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Cacheable(value = "dashboard",
               key = "#principal.organizationId",
               unless = "#result == null")
    @Operation(summary = "Get the executive compliance dashboard",
               description = "Returns overall risk score, violation counts by severity, "
                           + "per-framework scores, and the 10 most critical open violations. "
                           + "Cached for 2 minutes.")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam(required = false) UUID organizationId,
            @CurrentUser CompliancePrincipal principal) {

        UUID orgId = organizationId != null ? organizationId : principal.getOrganizationId();

        Organization org = organizationRepository
                .findByIdAndDeletedAtIsNull(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation", orgId));

        // ── Violation counts ──────────────────────────────────────
        List<Object[]> bySeverity  = violationRepository.countOpenBySeverity(orgId);
        List<Object[]> byFramework = violationRepository.countOpenByFramework(orgId);

        Map<String, Long> severityMap  = toMap(bySeverity);
        Map<String, Long> frameworkMap = toMap(byFramework);

        long criticalViolations = severityMap.getOrDefault("CRITICAL", 0L);
        long highViolations     = severityMap.getOrDefault("HIGH",     0L);
        long mediumViolations   = severityMap.getOrDefault("MEDIUM",   0L);
        long lowViolations      = severityMap.getOrDefault("LOW",      0L);
        long totalOpen          = criticalViolations + highViolations
                                + mediumViolations + lowViolations;

        // ── Policy counts ─────────────────────────────────────────
        List<Object[]> policyByStatus = policyRepository
                .countByStatusForOrganization(orgId);
        Map<String, Long> policyStatusMap = toMap(policyByStatus);

        // ── Alert counts ──────────────────────────────────────────
        long openAlerts     = alertRepository.countByOrganizationIdAndStatus(orgId, "OPEN");
        List<Object[]> alertBySeverity = alertRepository.countOpenBySeverity(orgId);
        Map<String, Long> alertSevMap  = toMap(alertBySeverity);

        // ── Per-framework compliance score ────────────────────────
        Map<String, BigDecimal> scoreByFramework = frameworkMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> computeFrameworkScore(e.getValue(), totalOpen)));

        // ── Recent violations (top 10 critical) ───────────────────
        List<ViolationResponse> recentViolations = violationRepository
                .findMostCriticalOpen(orgId, 10).stream()
                .map(v -> ViolationResponse.builder()
                        .id(v.getId())
                        .severity(v.getSeverity())
                        .status(v.getStatus())
                        .title(v.getTitle())
                        .policyName(v.getPolicy().getName())
                        .framework(v.getPolicy().getFramework().getValue())
                        .detectedAt(v.getDetectedAt())
                        .build())
                .toList();

        // ── Recent alerts (top 5) ─────────────────────────────────
        List<AlertResponse> recentAlerts = alertRepository
                .findRecent(orgId, 5).stream()
                .map(a -> AlertResponse.builder()
                        .id(a.getId())
                        .severity(a.getSeverity())
                        .status(a.getStatus())
                        .title(a.getTitle())
                        .createdAt(a.getCreatedAt())
                        .build())
                .toList();

        // ── Overall score (stored on org row after each evaluation) ─
        BigDecimal overallScore = org.getOverallRiskScore() != null
                ? org.getOverallRiskScore()
                : BigDecimal.valueOf(100.0 - (totalOpen * 2.0)).max(BigDecimal.ZERO);

        DashboardResponse dashboard = DashboardResponse.builder()
                .organizationId(orgId)
                .organizationName(org.getName())
                .overallComplianceScore(overallScore)
                .scoreTrend("STABLE")                   // updated by risk scoring scheduler
                .totalOpenViolations(totalOpen)
                .criticalViolations(criticalViolations)
                .highViolations(highViolations)
                .mediumViolations(mediumViolations)
                .lowViolations(lowViolations)
                .totalPolicies(policyStatusMap.values().stream().mapToLong(Long::longValue).sum())
                .activePolicies(policyStatusMap.getOrDefault("ACTIVE", 0L))
                .draftPolicies(policyStatusMap.getOrDefault("DRAFT", 0L))
                .openAlerts(openAlerts)
                .criticalAlerts(alertSevMap.getOrDefault("CRITICAL", 0L))
                .scoreByFramework(scoreByFramework)
                .recentViolations(recentViolations)
                .recentAlerts(recentAlerts)
                .lastEvaluatedAt(org.getRiskLastUpdated())
                .generatedAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    // ── GET /api/v1/dashboard/risk-scores ────────────────────────

    @GetMapping("/risk-scores")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get per-policy risk scores for the organisation")
    public ResponseEntity<ApiResponse<List<RiskScoreResponse>>> getRiskScores(
            @RequestParam(required = false) UUID organizationId,
            @CurrentUser CompliancePrincipal principal) {

        UUID orgId = organizationId != null ? organizationId : principal.getOrganizationId();

        List<RiskScoreResponse> scores = policyRepository
                .findAllByOrganizationIdAndStatusAndDeletedAtIsNull(
                        orgId, com.company.compliance.domain.enums.PolicyStatus.ACTIVE)
                .stream()
                .map(p -> riskScoringService.recalculate(p.getId(), orgId))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(scores));
    }

    // ── Private helpers ───────────────────────────────────────────

    private Map<String, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(
                Collectors.toMap(r -> r[0].toString(),
                                 r -> ((Number) r[1]).longValue()));
    }

    private BigDecimal computeFrameworkScore(long openViolations, long totalOpen) {
        if (totalOpen == 0) return BigDecimal.valueOf(100);
        double proportion = (double) openViolations / totalOpen;
        return BigDecimal.valueOf(Math.max(0, 100 - proportion * 100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
