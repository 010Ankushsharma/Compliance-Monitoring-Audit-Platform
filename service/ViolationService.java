package com.company.compliance.service;

import com.company.compliance.annotation.Auditable;
import com.company.compliance.domain.entity.ComplianceViolation;
import com.company.compliance.domain.entity.User;
import com.company.compliance.domain.enums.ViolationStatus;
import com.company.compliance.dto.request.UpdateViolationStatusRequest;
import com.company.compliance.dto.request.ViolationFilterRequest;
import com.company.compliance.dto.response.ViolationResponse;
import com.company.compliance.dto.response.ViolationSummaryResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.exception.InvalidStateTransitionException;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.repository.ComplianceViolationRepository;
import com.company.compliance.repository.UserRepository;
import com.company.compliance.security.CompliancePrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Compliance violation management service.
 *
 * <p>File: {@code src/main/java/com/company/compliance/service/ViolationService.java}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViolationService {

    private final ComplianceViolationRepository violationRepository;
    private final UserRepository                userRepository;
    private final RiskScoringService            riskScoringService;

    // ── Read ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ViolationResponse getViolation(UUID violationId, CompliancePrincipal principal) {
        ComplianceViolation v = violationRepository
                .findByIdAndOrganizationId(violationId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Violation", violationId));
        return toResponse(v);
    }

    @Transactional(readOnly = true)
    public PageResponse<ViolationResponse> listViolations(UUID organizationId,
                                                           ViolationFilterRequest req,
                                                           CompliancePrincipal principal) {
        if (!principal.canManage(organizationId)) {
            throw new AccessDeniedException("Access denied to organisation " + organizationId);
        }

        var pageable = PageRequest.of(req.getPage(), req.getSize(),
                Sort.by(req.getSortBy()).descending());

        var page = violationRepository.findWithFilters(
                organizationId,
                req.getPolicyId(),
                req.getUserId(),
                req.getSeverities() != null && req.getSeverities().size() == 1
                        ? req.getSeverities().get(0) : null,
                req.getStatuses() != null && req.getStatuses().size() == 1
                        ? req.getStatuses().get(0) : null,
                req.getFramework(),
                req.getDetectedFrom(),
                req.getDetectedTo(),
                pageable);

        return PageResponse.from(page.map(this::toResponse));
    }

    // ── Status Update ─────────────────────────────────────────────

    @Transactional
    @Auditable(action = "VIOLATION_STATUS_UPDATED", resourceType = "VIOLATION",
               resourceIdArg = "violationId")
    public ViolationResponse updateStatus(UUID violationId,
                                          UpdateViolationStatusRequest req,
                                          CompliancePrincipal principal) {
        ComplianceViolation violation = violationRepository
                .findByIdAndOrganizationId(violationId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Violation", violationId));

        ViolationStatus current = violation.getStatus();
        ViolationStatus target  = req.getStatus();

        if (!current.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(
                    "Cannot transition violation from " + current + " to " + target);
        }

        User actor = userRepository.findByIdAndDeletedAtIsNull(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));

        switch (target) {
            case IN_REVIEW     -> violation.acknowledge(actor);
            case RESOLVED      -> violation.resolve(actor, req.getNote());
            case FALSE_POSITIVE -> violation.markFalsePositive(actor, req.getNote());
            case SUPPRESSED    -> violation.setStatus(ViolationStatus.SUPPRESSED);
            case OPEN          -> violation.setStatus(ViolationStatus.OPEN); // re-open
            default            -> throw new InvalidStateTransitionException(
                    "Unsupported target status: " + target);
        }

        ComplianceViolation saved = violationRepository.save(violation);

        // Trigger async risk score recalculation
        riskScoringService.recalculateAsync(
                violation.getPolicy().getId(),
                violation.getOrganization().getId());

        log.info("Violation {} status changed {} → {} by {}", violationId,
                current, target, principal.getUserId());
        return toResponse(saved);
    }

    // ── Summary ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ViolationSummaryResponse getSummary(UUID organizationId,
                                                CompliancePrincipal principal) {
        if (!principal.canManage(organizationId)) {
            throw new AccessDeniedException("Access denied");
        }

        List<Object[]> bySeverity  = violationRepository.countOpenBySeverity(organizationId);
        List<Object[]> byFramework = violationRepository.countOpenByFramework(organizationId);
        List<Object[]> byPolicy    = violationRepository.countOpenByPolicy(organizationId);

        Map<String, Long> severityMap  = toStringLongMap(bySeverity);
        Map<String, Long> frameworkMap = toStringLongMap(byFramework);
        Map<String, Long> policyMap    = toPolicyMap(byPolicy);

        double avgResolution = Optional.ofNullable(
                violationRepository.averageResolutionHours(organizationId,
                        OffsetDateTime.now().minusDays(90)))
                .orElse(0.0);

        long totalOpen = severityMap.values().stream().mapToLong(Long::longValue).sum();

        return ViolationSummaryResponse.builder()
                .organizationId(organizationId)
                .totalOpen(totalOpen)
                .totalCritical(severityMap.getOrDefault("CRITICAL", 0L))
                .totalHigh(severityMap.getOrDefault("HIGH", 0L))
                .totalMedium(severityMap.getOrDefault("MEDIUM", 0L))
                .totalLow(severityMap.getOrDefault("LOW", 0L))
                .totalResolved(violationRepository.countByOrganizationIdAndStatus(
                        organizationId, ViolationStatus.RESOLVED))
                .totalFalsePositives(violationRepository.countByOrganizationIdAndStatus(
                        organizationId, ViolationStatus.FALSE_POSITIVE))
                .byFramework(frameworkMap)
                .byPolicy(policyMap)
                .averageResolutionHours(avgResolution)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────

    private ViolationResponse toResponse(ComplianceViolation v) {
        return ViolationResponse.builder()
                .id(v.getId())
                .organizationId(v.getOrganization().getId())
                .policyId(v.getPolicy().getId())
                .policyName(v.getPolicy().getName())
                .framework(v.getPolicy().getFramework().getValue())
                .policyRuleId(v.getPolicyRule() != null ? v.getPolicyRule().getId() : null)
                .policyRuleName(v.getPolicyRule() != null ? v.getPolicyRule().getName() : null)
                .userId(v.getUser() != null ? v.getUser().getId() : null)
                .userEmail(v.getUser() != null ? v.getUser().getEmail() : null)
                .severity(v.getSeverity())
                .status(v.getStatus())
                .title(v.getTitle())
                .description(v.getDescription())
                .evidence(v.getEvidence())
                .detectedAt(v.getDetectedAt())
                .acknowledgedById(v.getAcknowledgedBy() != null
                        ? v.getAcknowledgedBy().getId() : null)
                .acknowledgedAt(v.getAcknowledgedAt())
                .resolvedById(v.getResolvedBy() != null ? v.getResolvedBy().getId() : null)
                .resolvedAt(v.getResolvedAt())
                .resolutionNote(v.getResolutionNote())
                .riskScore(v.getRiskScore())
                .affectsRiskScore(v.affectsRiskScore())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }

    private Map<String, Long> toStringLongMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> r[0].toString(), r -> ((Number) r[1]).longValue()));
    }

    private Map<String, Long> toPolicyMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                r -> r[1].toString(), r -> ((Number) r[2]).longValue()));
    }
}
