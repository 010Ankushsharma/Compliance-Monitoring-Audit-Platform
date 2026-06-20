package com.company.compliance.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable, hash-chained audit log entry.
 *
 * <p><strong>Immutability is enforced at two layers:</strong>
 * <ol>
 *   <li>Database trigger {@code trg_audit_logs_immutable_*} prevents UPDATE/DELETE.</li>
 *   <li>This entity has no setters — all fields are set at construction time only.</li>
 * </ol>
 *
 * <p>The SHA-256 {@code hash} covers: id + timestamp + userId + action +
 * resourceType + resourceId + outcome + previousHash.
 * Chained via {@code previousHash} to detect tampering.
 *
 * <p>Maps to the {@code audit_logs} table.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA only
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(name = "timestamp", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", updatable = false)
    private User user;

    /** Denormalised email — preserved even if the user account is deleted. */
    @Column(name = "user_email", updatable = false, length = 255)
    private String userEmail;

    /** Action code, e.g. DATA_ACCESS, LOGIN, POLICY_CHANGE, REPORT_GENERATED. */
    @Column(name = "action", nullable = false, updatable = false, length = 100)
    private String action;

    @Column(name = "resource_type", updatable = false, length = 100)
    private String resourceType;

    @Column(name = "resource_id", updatable = false, length = 255)
    private String resourceId;

    @Column(name = "resource_name", updatable = false, length = 500)
    private String resourceName;

    @Column(name = "http_method", updatable = false, length = 10)
    private String httpMethod;

    @Column(name = "endpoint", updatable = false, length = 500)
    private String endpoint;

    @Column(name = "ip_address", updatable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", updatable = false, columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_id", updatable = false, length = 100)
    private String requestId;

    /** Outcome of the audited action. Values: SUCCESS, FAILURE, ERROR. */
    @Column(name = "outcome", nullable = false, updatable = false, length = 20)
    @Builder.Default
    private String outcome = "SUCCESS";

    @Column(name = "status_code", updatable = false)
    private Short statusCode;

    @Column(name = "duration_ms", updatable = false)
    private Integer durationMs;

    /** Arbitrary JSON context captured at audit time. */
    @Type(JsonType.class)
    @Column(name = "details", columnDefinition = "jsonb", updatable = false)
    private Map<String, Object> details;

    // ── Hash chain fields ─────────────────────────────────────────

    /** SHA-256 of this row's canonical fields. */
    @Column(name = "hash", nullable = false, updatable = false, length = 128)
    private String hash;

    /** SHA-256 hash of the immediately preceding audit_log row. */
    @Column(name = "previous_hash", updatable = false, length = 128)
    private String previousHash;

    /**
     * Monotonically increasing sequence number assigned by the DB
     * ({@code BIGSERIAL}). Used to walk the chain in order.
     * Never set by application code.
     */
    @Column(name = "sequence_number", updatable = false, insertable = false)
    private Long sequenceNumber;

    // ── Immutability guard ────────────────────────────────────────

    @PreUpdate
    protected void rejectUpdate() {
        throw new UnsupportedOperationException(
                "AuditLog entries are immutable and cannot be updated.");
    }

    @PreRemove
    protected void rejectDelete() {
        throw new UnsupportedOperationException(
                "AuditLog entries are immutable and cannot be deleted.");
    }
}
