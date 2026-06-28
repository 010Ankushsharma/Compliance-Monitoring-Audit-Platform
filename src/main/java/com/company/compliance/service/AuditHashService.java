package com.company.compliance.service;

import com.company.compliance.domain.entity.AuditLog;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.company.compliance.repository.AuditLogRepository;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the SHA-256 hash chain for immutable audit logs.
 *
 * <p>Every new audit entry is hashed over its canonical fields,
 * linked to the previous entry's hash, and persisted in a
 * SERIALIZABLE transaction to ensure monotonic sequence ordering.
 *
 * <p>Canonical hash input:
 * <pre>
 *   id | timestamp | userId | action | resourceType | resourceId | outcome | previousHash
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditHashService {

    private final AuditLogRepository auditLogRepository;

    private static final String GENESIS_HASH = "0".repeat(64); // hash of the first entry

    /**
     * Computes the hash chain link and persists the audit log entry.
     *
     * <p>Uses {@code REQUIRES_NEW} propagation so audit persistence
     * never rolls back with the caller's business transaction.
     *
     * @param entry  the partially constructed audit log (no hash yet)
     * @param orgId  organisation scope for chain lookup
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog persistWithHash(AuditLog entry, UUID orgId) {
        // Fetch the previous hash (last entry in this org's chain)
        String previousHash = auditLogRepository
                .findLatestByOrganization(orgId)
                .map(AuditLog::getHash)
                .orElse(GENESIS_HASH);

        // Compute this entry's hash
        String canonical = buildCanonicalString(entry, previousHash);
        String hash = sha256(canonical);

        // Use builder to create the final immutable entry
        AuditLog finalEntry = AuditLog.builder()
                .organization(entry.getOrganization())
                .timestamp(entry.getTimestamp())
                .user(entry.getUser())
                .userEmail(entry.getUserEmail())
                .action(entry.getAction())
                .resourceType(entry.getResourceType())
                .resourceId(entry.getResourceId())
                .resourceName(entry.getResourceName())
                .httpMethod(entry.getHttpMethod())
                .endpoint(entry.getEndpoint())
                .ipAddress(entry.getIpAddress())
                .userAgent(entry.getUserAgent())
                .requestId(entry.getRequestId())
                .outcome(entry.getOutcome())
                .statusCode(entry.getStatusCode())
                .durationMs(entry.getDurationMs())
                .details(entry.getDetails())
                .previousHash(previousHash)
                .hash(hash)
                .build();

        return auditLogRepository.save(finalEntry);
    }

    /**
     * Verifies the integrity of the hash chain for a given organisation.
     *
     * @return {@code true} if the chain is intact, {@code false} if tampering detected
     */
    @Transactional(readOnly = true)
    public boolean verifyChain(UUID orgId) {
        var entries = auditLogRepository.findChainForVerification(orgId, null, null);
        if (entries.isEmpty()) return true;

        String expectedPreviousHash = GENESIS_HASH;
        for (AuditLog entry : entries) {
            // Check previous_hash pointer
            String actualPreviousHash = Optional.ofNullable(entry.getPreviousHash())
                    .orElse(GENESIS_HASH);
            if (!expectedPreviousHash.equals(actualPreviousHash)) {
                log.error("Hash chain broken at entry id={}, seq={}",
                        entry.getId(), entry.getSequenceNumber());
                return false;
            }
            // Recompute and verify this entry's hash
            String canonical    = buildCanonicalString(entry, actualPreviousHash);
            String recomputed   = sha256(canonical);
            if (!recomputed.equals(entry.getHash())) {
                log.error("Hash mismatch at entry id={} — possible tampering", entry.getId());
                return false;
            }
            expectedPreviousHash = entry.getHash();
        }
        return true;
    }

    // ── Private ───────────────────────────────────────────────────

    private String buildCanonicalString(AuditLog entry, String previousHash) {
        return String.join("|",
                nullSafe(entry.getId()),
                nullSafe(entry.getTimestamp()),
                nullSafe(entry.getUserEmail()),
                nullSafe(entry.getAction()),
                nullSafe(entry.getResourceType()),
                nullSafe(entry.getResourceId()),
                nullSafe(entry.getOutcome()),
                previousHash
        );
    }

    private String sha256(String input) {
        return Hashing.sha256()
                .hashString(input, StandardCharsets.UTF_8)
                .toString();
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
