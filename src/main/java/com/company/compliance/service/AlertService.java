package com.company.compliance.service;

import com.company.compliance.domain.entity.Alert;
import com.company.compliance.domain.entity.ComplianceViolation;
import com.company.compliance.domain.enums.Severity;
import com.company.compliance.dto.response.AlertResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.repository.AlertRepository;
import com.company.compliance.repository.UserRepository;
import com.company.compliance.security.CompliancePrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Alert lifecycle management and notification dispatch service.
 *
 * <p>File: {@code src/main/java/com/company/compliance/service/AlertService.java}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int NOTIFICATION_BATCH_SIZE = 50;

    private final AlertRepository       alertRepository;
    private final UserRepository        userRepository;
    private final NotificationService   notificationService;

    // ── Create from violation ─────────────────────────────────────

    /**
     * Creates an alert from a detected violation.
     * Deduplication: if an open alert with the same dedupKey exists, skips creation.
     */
    @Transactional
    public Alert createFromViolation(ComplianceViolation violation) {
        String dedupKey = buildDedupKey(violation);

        if (alertRepository.existsByDedupKeyAndStatusNot(dedupKey, "RESOLVED")) {
            log.debug("Alert deduplication: skipping duplicate for violation {}",
                    violation.getId());
            return alertRepository.findByDedupKeyAndStatusNot(dedupKey, "RESOLVED")
                    .orElseThrow();
        }

        Alert alert = Alert.builder()
                .organization(violation.getOrganization())
                .violation(violation)
                .severity(violation.getSeverity())
                .status("OPEN")
                .title(violation.getTitle())
                .message(buildAlertMessage(violation))
                .source("ViolationDetectionEngine")
                .dedupKey(dedupKey)
                .notificationSent(false)
                .build();

        Alert saved = alertRepository.save(alert);
        log.info("Alert created [id={}] for violation [id={}] severity={}",
                saved.getId(), violation.getId(), violation.getSeverity());
        return saved;
    }

    // ── Read ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AlertResponse getAlert(UUID alertId, CompliancePrincipal principal) {
        Alert alert = alertRepository.findByIdAndOrganizationId(
                alertId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));
        return toResponse(alert);
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> listAlerts(UUID organizationId,
                                                   String status, Severity severity,
                                                   OffsetDateTime from, OffsetDateTime to,
                                                   int page, int size,
                                                   CompliancePrincipal principal) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result   = alertRepository.findWithFilters(
                organizationId, status, severity, from, to, pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    // ── Status management ─────────────────────────────────────────

    @Transactional
    public AlertResponse acknowledgeAlert(UUID alertId, CompliancePrincipal principal) {
        Alert alert = resolveAlertForOrg(alertId, principal);
        var actor = userRepository.findByIdAndDeletedAtIsNull(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));
        alert.acknowledge(actor);
        return toResponse(alertRepository.save(alert));
    }

    @Transactional
    public AlertResponse resolveAlert(UUID alertId, CompliancePrincipal principal) {
        Alert alert = resolveAlertForOrg(alertId, principal);
        var actor = userRepository.findByIdAndDeletedAtIsNull(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));
        alert.resolve(actor);
        return toResponse(alertRepository.save(alert));
    }

    // ── Notification dispatch (scheduled every 30s) ───────────────

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void dispatchPendingNotifications() {
        List<Alert> pending = alertRepository.findPendingNotifications(
                OffsetDateTime.now(), NOTIFICATION_BATCH_SIZE);

        if (pending.isEmpty()) return;
        log.debug("Dispatching notifications for {} pending alerts", pending.size());

        for (Alert alert : pending) {
            try {
                String[] channelsSent = notificationService.send(alert);
                alert.markNotificationSent(channelsSent);
                alertRepository.save(alert);
            } catch (Exception e) {
                log.error("Failed to send notification for alert {}: {}",
                        alert.getId(), e.getMessage());
            }
        }
    }

    // ── Escalation (scheduled every 15 minutes) ───────────────────

    @Scheduled(fixedDelay = 900_000)
    @Transactional
    public void escalateStaleAlerts() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Alert> alerts = alertRepository.findAlertsRequiringEscalation(
                3,                               // max escalation level
                now.minusHours(1),               // CRITICAL threshold
                now.minusHours(4),               // HIGH threshold
                now.minusHours(24));             // default threshold

        for (Alert alert : alerts) {
            alert.escalate();
            alertRepository.save(alert);
            log.warn("Alert {} escalated to level {} (severity={})",
                    alert.getId(), alert.getEscalationLevel(), alert.getSeverity());
        }
    }

    // ── Private helpers ───────────────────────────────────────────

    private Alert resolveAlertForOrg(UUID alertId, CompliancePrincipal principal) {
        return alertRepository.findByIdAndOrganizationId(
                alertId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));
    }

    private String buildDedupKey(ComplianceViolation violation) {
        return "violation:" + violation.getPolicy().getId()
                + ":" + (violation.getPolicyRule() != null
                        ? violation.getPolicyRule().getId() : "norule")
                + ":" + (violation.getUser() != null
                        ? violation.getUser().getId() : "nouser");
    }

    private String buildAlertMessage(ComplianceViolation violation) {
        return String.format(
                "[%s] Policy violation detected: %s\nFramework: %s\nDetected at: %s",
                violation.getSeverity(),
                violation.getTitle(),
                violation.getPolicy().getFramework().getDisplayName(),
                violation.getDetectedAt());
    }

    private AlertResponse toResponse(Alert a) {
        return AlertResponse.builder()
                .id(a.getId())
                .organizationId(a.getOrganization().getId())
                .violationId(a.getViolation() != null ? a.getViolation().getId() : null)
                .severity(a.getSeverity())
                .status(a.getStatus())
                .title(a.getTitle())
                .message(a.getMessage())
                .source(a.getSource())
                .dedupKey(a.getDedupKey())
                .escalationLevel(a.getEscalationLevel())
                .escalatedAt(a.getEscalatedAt())
                .acknowledgedById(a.getAcknowledgedBy() != null
                        ? a.getAcknowledgedBy().getId() : null)
                .acknowledgedAt(a.getAcknowledgedAt())
                .resolvedAt(a.getResolvedAt())
                .notificationSent(a.isNotificationSent())
                .notificationSentAt(a.getNotificationSentAt())
                .notificationChannels(a.getNotificationChannels())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
