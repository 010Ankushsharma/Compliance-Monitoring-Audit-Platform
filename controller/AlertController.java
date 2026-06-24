package com.company.compliance.controller;

import com.company.compliance.domain.enums.Severity;
import com.company.compliance.dto.common.ApiResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.dto.response.AlertResponse;
import com.company.compliance.security.CompliancePrincipal;
import com.company.compliance.security.CurrentUser;
import com.company.compliance.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Alert management REST controller.
 *
 * <p>File: {@code src/main/java/com/company/compliance/controller/AlertController.java}
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Alert listing, acknowledgement, and resolution")
public class AlertController {

    private final AlertService alertService;

    // ── GET /api/v1/alerts ────────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List alerts with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<AlertResponse>>> listAlerts(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser CompliancePrincipal principal) {

        UUID orgId = organizationId != null ? organizationId : principal.getOrganizationId();
        return ResponseEntity.ok(ApiResponse.success(
                alertService.listAlerts(orgId, status, severity, from, to, page, size, principal)));
    }

    // ── GET /api/v1/alerts/{id} ───────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get an alert by ID")
    public ResponseEntity<ApiResponse<AlertResponse>> getAlert(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                alertService.getAlert(id, principal)));
    }

    // ── POST /api/v1/alerts/{id}/acknowledge ─────────────────────

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR')")
    @Operation(summary = "Acknowledge an open alert")
    public ResponseEntity<ApiResponse<AlertResponse>> acknowledge(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success("Alert acknowledged",
                alertService.acknowledgeAlert(id, principal)));
    }

    // ── POST /api/v1/alerts/{id}/resolve ─────────────────────────

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR')")
    @Operation(summary = "Resolve an alert")
    public ResponseEntity<ApiResponse<AlertResponse>> resolve(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success("Alert resolved",
                alertService.resolveAlert(id, principal)));
    }
}
