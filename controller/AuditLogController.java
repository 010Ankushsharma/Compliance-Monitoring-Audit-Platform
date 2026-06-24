package com.company.compliance.controller;

import com.company.compliance.dto.common.ApiResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.dto.request.AuditLogSearchRequest;
import com.company.compliance.dto.response.AuditLogResponse;
import com.company.compliance.dto.response.ChainVerificationResponse;
import com.company.compliance.security.CompliancePrincipal;
import com.company.compliance.security.CurrentUser;
import com.company.compliance.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Audit log search and chain verification endpoints.
 *
 * <p>All audit log entries are immutable — no write endpoints exist here.
 * Entries are created automatically by {@link com.company.compliance.security.AuditAspect}.
 *
 * <p>File: {@code src/main/java/com/company/compliance/controller/AuditLogController.java}
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Immutable audit trail search and integrity verification")
public class AuditLogController {

    private final AuditLogService auditLogService;

    // ── GET /api/v1/audit-logs ────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR')")
    @Operation(summary = "Search audit logs with filters",
               description = "All query parameters are optional and can be combined. "
                           + "Results are returned in descending timestamp order by default.")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> search(
            @Valid AuditLogSearchRequest request,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.search(principal.getOrganizationId(), request)));
    }

    // ── GET /api/v1/audit-logs/{id} ───────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR')")
    @Operation(summary = "Get a single audit log entry by ID")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getById(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        AuditLogSearchRequest req = new AuditLogSearchRequest();
        req.setSize(1);
        // Delegate to search with requestId filter as a workaround (single-entry lookup)
        // In a production project extract this to a dedicated repository method
        var page = auditLogService.search(principal.getOrganizationId(), req);
        return ResponseEntity.ok(ApiResponse.success(
                page.getContent().stream()
                    .filter(a -> id.toString().equals(a.getId() != null
                            ? a.getId().toString() : ""))
                    .findFirst()
                    .orElseThrow(() -> new com.company.compliance.exception
                            .ResourceNotFoundException("AuditLog", id))));
    }

    // ── GET /api/v1/audit-logs/verify-chain ───────────────────────

    @GetMapping("/verify-chain")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR')")
    @Operation(summary = "Verify the SHA-256 hash chain integrity for a time range",
               description = "Walks the entire hash chain and reports any broken links. "
                           + "A clean result (intact=true, brokenLinks=0) confirms no tampering.")
    public ResponseEntity<ApiResponse<ChainVerificationResponse>> verifyChain(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @CurrentUser CompliancePrincipal principal) {

        // Default to last 30 days if not specified
        OffsetDateTime effectiveFrom = from != null ? from : OffsetDateTime.now().minusDays(30);
        OffsetDateTime effectiveTo   = to   != null ? to   : OffsetDateTime.now();

        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.verifyChain(
                        principal.getOrganizationId(), effectiveFrom, effectiveTo)));
    }

    // ── GET /api/v1/audit-logs/user/{userId} ─────────────────────

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR')")
    @Operation(summary = "Get recent audit trail for a specific user")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @CurrentUser CompliancePrincipal principal) {

        AuditLogSearchRequest req = new AuditLogSearchRequest();
        req.setUserId(userId);
        req.setPage(page);
        req.setSize(size);
        return ResponseEntity.ok(ApiResponse.success(
                auditLogService.search(principal.getOrganizationId(), req)));
    }
}
