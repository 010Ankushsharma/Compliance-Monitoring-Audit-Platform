package com.company.compliance.controller;

import com.company.compliance.config.AppProperties;
import com.company.compliance.dto.common.ApiResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.dto.request.GenerateReportRequest;
import com.company.compliance.dto.response.ReportResponse;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.repository.ReportRepository;
import com.company.compliance.security.CompliancePrincipal;
import com.company.compliance.security.CurrentUser;
import com.company.compliance.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Report generation and download REST controller.
 *
 * <p>File: {@code src/main/java/com/company/compliance/controller/ReportController.java}
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Async compliance report generation and download")
public class ReportController {

    private final ReportService      reportService;
    private final ReportRepository   reportRepository;
    private final AppProperties      appProperties;

    // ── POST /api/v1/reports ──────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR')")
    @Operation(summary = "Request a new compliance report",
               description = "Report generation is asynchronous. Poll GET /api/v1/reports/{id} "
                           + "until status = COMPLETED, then use the downloadUrl.")
    public ResponseEntity<ApiResponse<ReportResponse>> requestReport(
            @Valid @RequestBody GenerateReportRequest request,
            @CurrentUser CompliancePrincipal principal) {

        ReportResponse report = reportService.requestReport(request, principal);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Report generation queued", report));
    }

    // ── GET /api/v1/reports ───────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List reports for the authenticated organisation")
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> listReports(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String format,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser CompliancePrincipal principal) {

        UUID orgId = organizationId != null ? organizationId : principal.getOrganizationId();
        return ResponseEntity.ok(ApiResponse.success(
                reportService.listReports(orgId, status, format, page, size, principal)));
    }

    // ── GET /api/v1/reports/{id} ──────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get report metadata and generation status")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                reportService.getReport(id, principal)));
    }

    // ── GET /api/v1/reports/{id}/download ────────────────────────

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download a completed report file (PDF / Excel / CSV / JSON)")
    public ResponseEntity<Resource> downloadReport(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        var report = reportRepository.findByIdAndOrganizationId(
                id, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Report", id));

        if (!"COMPLETED".equals(report.getStatus()) || report.getFilePath() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // 409 — not ready
        }

        File file = new File(report.getFilePath());
        if (!file.exists()) {
            log.error("Report file missing on disk: {}", report.getFilePath());
            return ResponseEntity.status(HttpStatus.GONE).build(); // 410 — file purged
        }

        Resource resource = new FileSystemResource(file);
        String contentType = resolveContentType(report.getFormat());
        String disposition  = "attachment; filename=\"" + file.getName() + "\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(file.length())
                .body(resource);
    }

    private String resolveContentType(String format) {
        return switch (format.toUpperCase()) {
            case "PDF"   -> "application/pdf";
            case "EXCEL" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "CSV"   -> "text/csv";
            case "JSON"  -> "application/json";
            default      -> "application/octet-stream";
        };
    }
}
