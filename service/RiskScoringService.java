package com.company.compliance.service;

import com.company.compliance.domain.entity.ComplianceViolation;
import com.company.compliance.domain.entity.Policy;
import com.company.compliance.domain.enums.Severity;
import com.company.compliance.dto.response.RiskScoreResponse;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.repository.ComplianceViolationRepository;
import com.company.compliance.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Computes compliance / risk scores for policies.
 *
 * <p>Score algorithm (0 = non-compliant, 100 = fully compliant):
 * <pre>
 *   baseScore = 100
 *   for each open violation:
 *     penalty += severity.getRiskWeight() * (1 + 0.1 * count_at_same_severity)
 *   complianceScore = max(0, baseScore - totalPenalty)
 * </pre>
 *
 * <p>File: {@code src/main/java/com/company/compliance/service/RiskScoringService.java}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private final ComplianceViolationRepository violationRepository;
    private final PolicyRepository              policyRepository;

    /**
     * Recalculates risk score for a policy asynchronously.
     * Called after any violation status change.
     */
    @Async("taskExecutor")
    @CacheEvict(value = "risk-scores", allEntries = true)
    public void recalculateAsync(UUID policyId, UUID organizationId) {
        try {
            recalculate(policyId, organizationId);
        } catch (Exception e) {
            log.error("Async risk recalculation failed for policy {}: {}", policyId, e.getMessage());
        }
    }

    @Transactional
    @CacheEvict(value = {"risk-scores", "dashboard"}, allEntries = true)
    public RiskScoreResponse recalculate(UUID policyId, UUID organizationId) {
        Policy policy = policyRepository.findByIdAndDeletedAtIsNull(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", policyId));

        List<ComplianceViolation> openViolations =
                violationRepository.findOpenByPolicy(policyId);

        // Count by severity
        long critical = count(openViolations, Severity.CRITICAL);
        long high     = count(openViolations, Severity.HIGH);
        long medium   = count(openViolations, Severity.MEDIUM);
        long low      = count(openViolations, Severity.LOW);

        // Compute total penalty
        double penalty = 0;
        penalty += critical * Severity.CRITICAL.getRiskWeight() * (1 + 0.1 * (critical - 1));
        penalty += high     * Severity.HIGH.getRiskWeight()     * (1 + 0.1 * (high - 1));
        penalty += medium   * Severity.MEDIUM.getRiskWeight()   * (1 + 0.1 * (medium - 1));
        penalty += low      * Severity.LOW.getRiskWeight()      * (1 + 0.1 * (low - 1));
        penalty = Math.min(penalty, 100.0);

        double score = Math.max(0, 100.0 - penalty);

        log.info("Risk score for policy {} [{}]: {:.2f} (violations: C={}, H={}, M={}, L={})",
                policy.getName(), policyId, score, critical, high, medium, low);

        return RiskScoreResponse.builder()
                .policyId(policyId)
                .policyName(policy.getName())
                .organizationId(organizationId)
                .framework(policy.getFramework().getValue())
                .complianceScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP))
                .violationPenalty(BigDecimal.valueOf(penalty).setScale(2, RoundingMode.HALF_UP))
                .openViolationsCount((int) openViolations.size())
                .criticalViolations((int) critical)
                .highViolations((int) high)
                .mediumViolations((int) medium)
                .lowViolations((int) low)
                .build();
    }

    private long count(List<ComplianceViolation> violations, Severity severity) {
        return violations.stream()
                .filter(v -> severity == v.getSeverity())
                .count();
    }
}
