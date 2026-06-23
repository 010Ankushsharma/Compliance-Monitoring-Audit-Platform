package com.company.compliance.service;

import com.company.compliance.config.AppProperties;
import com.company.compliance.domain.entity.AuditLog;
import com.company.compliance.domain.entity.Organization;
import com.company.compliance.domain.entity.User;
import com.company.compliance.dto.request.AuditLogSearchRequest;
import com.company.compliance.dto.response.AuditLogResponse;
import com.company.compliance.dto.response.ChainVerificationResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.event.AuditEvent;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.repository.AuditLogRepository;
import com.company.compliance.repository.OrganizationRepository;
import com.company.compliance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Immutable audit log service — handles publication and search of audit events.
 *
 * <p>Two publication modes (controlled by {@code app.audit.async-enabled}):
 * <ul>
 *   <li><strong>Async (prod):</strong> event sent to Kafka topic; consumer writes to DB
 *       on a separate thread. Zero latency impact on the request thread.</li>
 *   <li><strong>Sync (dev):</strong> directly writes to the DB in the calling thread.
 *       Simpler for local debugging.</li>
 * </ul>
 *
 * <p>File: {@code src/main/java/com/company/compliance/service/AuditLogService.java}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository     auditLogRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository         userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppProperties          appProperties;

    // ── Event publication ─────────────────────────────────────────

    /**
     * Publishes an audit event asynchronously (called by {@link com.company.compliance.security.AuditAspect}).
     * Method is {@code @Async} — always runs on the {@code auditExecutor} pool.
     */
    @Async("auditExecutor")
    public void publishAuditEvent(UUID organizationId, UUID userId, String userEmail,
                                  String action, String resourceType, String resourceId,
                                  String httpMethod, String endpoint, String ipAddress,
                                  String userAgent, String requestId,
                                  String outcome, short statusCode, int durationMs,
                                  Map<String, Object> details) {
        try {
            if (appProperties.getAudit().isAsyncEnabled()) {
                // Send to Kafka — AuditEventConsumer persists it
                AuditEvent event = AuditEvent.builder()
                        .organizationId(organizationId)
                        .userId(userId)
                        .userEmail(userEmail)
                        .action(action)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .httpMethod(httpMethod)
                        .endpoint(endpoint)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .requestId(requestId)
                        .outcome(outcome)
                        .statusCode(statusCode)
                        .durationMs(durationMs)
                        .details(details)
                        .timestamp(OffsetDateTime.now())
                        .build();

                kafkaTemplate.send(
                        appProperties.getKafka().getAuditEvents(),
                        organizationId != null ? organizationId.toString() : "system",
                        event);
            } else {
                // Direct DB write for dev mode
                persistAuditLog(organizationId, userId, userEmail, action, resourceType,
                        resourceId, httpMethod, endpoint, ipAddress, userAgent,
                        requestId, outcome, statusCode, durationMs, details);
            }
        } catch (Exception e) {
            log.error("Failed to publish audit event [{}]: {}", action, e.getMessage(), e);
        }
    }

    /**
     * Directly persists an audit log entry with hash chaining.
     * Called by the Kafka consumer in async mode, or directly in dev mode.
     */
    @Transactional
    public AuditLog persistAuditLog(UUID organizationId, UUID userId, String userEmail,
                                    String action, String resourceType, String resourceId,
                                    String httpMethod, String endpoint, String ipAddress,
                                    String userAgent, String requestId,
                                    String outcome, short statusCode, int durationMs,
                                    Map<String, Object> details) {

        Organization org = organizationId != null
                ? organizationRepository.findByIdAndDeletedAtIsNull(organizationId).orElse(null)
                : null;

        User user = userId != null
                ? userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null)
                : null;

        // Fetch previous hash for chain
        String previousHash = org != null
                ? auditLogRepository.findLatestByOrganization(organizationId)
                        .map(AuditLog::getHash)
                        .orElse("GENESIS")
                : "GENESIS";

        UUID entryId = UUID.randomUUID();

        // Compute hash of this entry's canonical fields
        String hash = computeHash(entryId, OffsetDateTime.now(), userId, action,
                resourceType, resourceId, outcome, previousHash);

        AuditLog entry = AuditLog.builder()
                .id(entryId)
                .organization(org)
                .user(user)
                .userEmail(userEmail)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .httpMethod(httpMethod)
                .endpoint(endpoint)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestId(requestId)
                .outcome(outcome)
                .statusCode(statusCode)
                .durationMs(durationMs)
                .details(details)
                .hash(hash)
                .previousHash(previousHash)
                .build();

        return auditLogRepository.save(entry);
    }

    // ── Search ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(UUID organizationId,
                                                  AuditLogSearchRequest req) {
        var pageable = PageRequest.of(
                req.getPage(), req.getSize(),
                "ASC".equalsIgnoreCase(req.getSortDir())
                        ? Sort.by(req.getSortBy()).ascending()
                        : Sort.by(req.getSortBy()).descending());

        var page = auditLogRepository.search(
                organizationId,
                req.getUserId(),
                req.getActions() != null && req.getActions().size() == 1
                        ? req.getActions().get(0) : null,
                req.getResourceType(),
                req.getResourceId(),
                req.getOutcome(),
                req.getRequestId(),
                req.getIpAddress(),
                req.getFrom(),
                req.getTo(),
                pageable);

        return PageResponse.from(page.map(this::toResponse));
    }

    // ── Chain verification ────────────────────────────────────────

    /**
     * Walks the full hash chain for an organisation and verifies integrity.
     * Reports any broken links (tampered entries).
     */
    @Transactional(readOnly = true)
    public ChainVerificationResponse verifyChain(UUID organizationId,
                                                  OffsetDateTime from,
                                                  OffsetDateTime to) {
        List<AuditLog> chain = auditLogRepository.findChainForVerification(
                organizationId, from, to);

        List<UUID> brokenLinks = new ArrayList<>();
        String expectedPreviousHash = "GENESIS";

        for (AuditLog entry : chain) {
            if (!expectedPreviousHash.equals(entry.getPreviousHash())) {
                brokenLinks.add(entry.getId());
                log.warn("Audit chain broken at entry {} (sequence {})",
                        entry.getId(), entry.getSequenceNumber());
            }
            // Recompute and verify the entry's own hash
            String recomputed = computeHash(
                    entry.getId(), entry.getTimestamp(),
                    entry.getUser() != null ? entry.getUser().getId() : null,
                    entry.getAction(), entry.getResourceType(), entry.getResourceId(),
                    entry.getOutcome(), entry.getPreviousHash());

            if (!recomputed.equals(entry.getHash())) {
                brokenLinks.add(entry.getId());
                log.warn("Hash mismatch at audit entry {} — possible tampering!", entry.getId());
            }
            expectedPreviousHash = entry.getHash();
        }

        return ChainVerificationResponse.builder()
                .intact(brokenLinks.isEmpty())
                .totalEntries(chain.size())
                .brokenLinks(brokenLinks.size())
                .brokenEntryIds(brokenLinks)
                .verifiedFrom(from)
                .verifiedTo(to)
                .verifiedAt(OffsetDateTime.now())
                .algorithm("SHA-256")
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────

    private String computeHash(UUID id, OffsetDateTime timestamp, UUID userId,
                                String action, String resourceType, String resourceId,
                                String outcome, String previousHash) {
        String canonical = String.join("|",
                id.toString(),
                timestamp.toString(),
                userId != null ? userId.toString() : "NULL",
                action != null ? action : "NULL",
                resourceType != null ? resourceType : "NULL",
                resourceId != null ? resourceId : "NULL",
                outcome != null ? outcome : "NULL",
                previousHash != null ? previousHash : "NULL");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .organizationId(a.getOrganization() != null ? a.getOrganization().getId() : null)
                .timestamp(a.getTimestamp())
                .userId(a.getUser() != null ? a.getUser().getId() : null)
                .userEmail(a.getUserEmail())
                .action(a.getAction())
                .resourceType(a.getResourceType())
                .resourceId(a.getResourceId())
                .resourceName(a.getResourceName())
                .httpMethod(a.getHttpMethod())
                .endpoint(a.getEndpoint())
                .ipAddress(a.getIpAddress())
                .requestId(a.getRequestId())
                .outcome(a.getOutcome())
                .statusCode(a.getStatusCode())
                .durationMs(a.getDurationMs())
                .details(a.getDetails())
                .hash(a.getHash())
                .previousHash(a.getPreviousHash())
                .sequenceNumber(a.getSequenceNumber())
                .build();
    }
}
