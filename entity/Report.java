package com.company.compliance.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * A generated compliance evidence report.
 *
 * <p>Reports are generated asynchronously. A Kafka message triggers the
 * {@code ReportGenerationConsumer}, which updates {@code status} from
 * {@code PENDING → GENERATING → COMPLETED | FAILED}.
 *
 * <p>Maps to the {@code reports} table.
 */
@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"organization", "generatedBy"})
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "template_id", nullable = false, length = 100)
    private String templateId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Generation status. Values: PENDING, GENERATING, COMPLETED, FAILED. */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    /** Output format. Values: PDF, EXCEL, CSV, JSON. */
    @Column(name = "format", nullable = false, length = 10)
    @Builder.Default
    private String format = "PDF";

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "include_evidence", nullable = false)
    @Builder.Default
    private boolean includeEvidence = true;

    /** Local file path or S3 object key once generation is complete. */
    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Summary statistics embedded for fast dashboard reads without
     * parsing the actual report file.
     */
    @Type(JsonType.class)
    @Column(name = "summary", columnDefinition = "jsonb")
    private Map<String, Object> summary;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // ── Lifecycle ─────────────────────────────────────────────────

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // ── Domain helpers ────────────────────────────────────────────

    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isGenerating() { return "GENERATING".equals(status); }
    public boolean isCompleted() { return "COMPLETED".equals(status); }
    public boolean isFailed() { return "FAILED".equals(status); }

    public void markStarted() {
        this.status = "GENERATING";
        this.startedAt = OffsetDateTime.now();
    }

    public void markCompleted(String filePath, long fileSizeBytes, Map<String, Object> summary) {
        this.status = "COMPLETED";
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.summary = summary;
        this.completedAt = OffsetDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = "FAILED";
        this.errorMessage = errorMessage;
        this.completedAt = OffsetDateTime.now();
    }
}
