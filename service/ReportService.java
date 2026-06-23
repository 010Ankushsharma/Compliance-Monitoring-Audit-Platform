package com.company.compliance.service;

import com.company.compliance.annotation.Auditable;
import com.company.compliance.domain.entity.Organization;
import com.company.compliance.domain.entity.Report;
import com.company.compliance.domain.entity.User;
import com.company.compliance.dto.request.GenerateReportRequest;
import com.company.compliance.dto.response.ReportResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.event.ReportRequestEvent;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.repository.OrganizationRepository;
import com.company.compliance.repository.ReportRepository;
import com.company.compliance.repository.UserRepository;
import com.company.compliance.security.CompliancePrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Report request and lifecycle management service.
 *
 * <p>Generation is always async: this service creates the Report row in PENDING state,
 * then sends a Kafka message. The {@code ReportGenerationConsumer} picks it up and
 * transitions the row to GENERATING → COMPLETED | FAILED.
 *
 * <p>File: {@code src/main/java/com/company/compliance/service/ReportService.java}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository       reportRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository         userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Request generation ────────────────────────────────────────

    @Transactional
    @Auditable(action = "REPORT_REQUESTED", resourceType = "REPORT")
    public ReportResponse requestReport(GenerateReportRequest req,
                                        CompliancePrincipal principal) {
        Organization org = organizationRepository
                .findByIdAndDeletedAtIsNull(principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organisation", principal.getOrganizationId()));

        User generatedBy = userRepository
                .findByIdAndDeletedAtIsNull(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getUserId()));

        Report report = Report.builder()
                .organization(org)
                .templateId(req.getTemplateId())
                .title(req.getTitle())
                .description(req.getDescription())
                .status("PENDING")
                .format(req.getFormat())
                .periodStart(req.getPeriodStart())
                .periodEnd(req.getPeriodEnd())
                .includeEvidence(req.isIncludeEvidence())
                .generatedBy(generatedBy)
                .build();

        Report saved = reportRepository.save(report);

        // Publish async generation request to Kafka
        ReportRequestEvent event = ReportRequestEvent.builder()
                .reportId(saved.getId())
                .organizationId(org.getId())
                .templateId(req.getTemplateId())
                .format(req.getFormat())
                .periodStart(req.getPeriodStart())
                .periodEnd(req.getPeriodEnd())
                .includeEvidence(req.isIncludeEvidence())
                .build();

        kafkaTemplate.send("compliance.report-requests",
                org.getId().toString(), event);

        log.info("Report [id={}] queued for generation by user {}", saved.getId(),
                principal.getUserId());
        return toResponse(saved, null);
    }

    // ── Read ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReportResponse getReport(UUID reportId, CompliancePrincipal principal) {
        Report report = reportRepository
                .findByIdAndOrganizationId(reportId, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Report", reportId));
        return toResponse(report, buildDownloadUrl(report));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportResponse> listReports(UUID organizationId,
                                                     String status, String format,
                                                     int page, int size,
                                                     CompliancePrincipal principal) {
        if (!principal.canManage(organizationId)) {
            throw new AccessDeniedException("Access denied to organisation " + organizationId);
        }
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var result   = reportRepository.findWithFilters(organizationId, status, format, pageable);
        return PageResponse.from(result.map(r -> toResponse(r, buildDownloadUrl(r))));
    }

    // ── Private helpers ───────────────────────────────────────────

    private String buildDownloadUrl(Report report) {
        if ("COMPLETED".equals(report.getStatus()) && report.getFilePath() != null) {
            return "/api/v1/reports/" + report.getId() + "/download";
        }
        return null;
    }

    private ReportResponse toResponse(Report r, String downloadUrl) {
        return ReportResponse.builder()
                .id(r.getId())
                .organizationId(r.getOrganization().getId())
                .templateId(r.getTemplateId())
                .title(r.getTitle())
                .description(r.getDescription())
                .status(r.getStatus())
                .format(r.getFormat())
                .periodStart(r.getPeriodStart())
                .periodEnd(r.getPeriodEnd())
                .includeEvidence(r.isIncludeEvidence())
                .fileSizeBytes(r.getFileSizeBytes())
                .generatedById(r.getGeneratedBy().getId())
                .generatedByName(r.getGeneratedBy().getFullName())
                .startedAt(r.getStartedAt())
                .completedAt(r.getCompletedAt())
                .errorMessage(r.getErrorMessage())
                .summary(r.getSummary())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .downloadUrl(downloadUrl)
                .build();
    }
}
