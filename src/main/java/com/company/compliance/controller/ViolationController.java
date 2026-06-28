package com.company.compliance.controller;

import com.company.compliance.dto.common.ApiResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.dto.request.UpdateViolationStatusRequest;
import com.company.compliance.dto.request.ViolationFilterRequest;
import com.company.compliance.dto.response.ViolationResponse;
import com.company.compliance.dto.response.ViolationSummaryResponse;
import com.company.compliance.security.CompliancePrincipal;
import com.company.compliance.security.CurrentUser;
import com.company.compliance.service.ViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Compliance violation management REST controller.
 *
 * <p>File: {@code src/main/java/com/company/compliance/controller/ViolationController.java}
 */
@RestController
@RequestMapping("/api/v1/violations")
@RequiredArgsConstructor
@Tag(name = "Violations", description = "Compliance violation tracking and workflow management")
public class ViolationController {

    private final ViolationService violationService;

    // ── GET /api/v1/violations ────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List violations with filters",
               description = "Filter by severity, status, policy, framework, and date range. "
                           + "Sorted by detected time descending by default.")
    public ResponseEntity<ApiResponse<PageResponse<ViolationResponse>>> listViolations(
            @RequestParam(required = false) UUID organizationId,
            @Valid ViolationFilterRequest filter,
            @CurrentUser CompliancePrincipal principal) {

        UUID orgId = organizationId != null ? organizationId : principal.getOrganizationId();
        return ResponseEntity.ok(ApiResponse.success(
                violationService.listViolations(orgId, filter, principal)));
    }

    // ── GET /api/v1/violations/{id} ───────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a violation by ID")
    public ResponseEntity<ApiResponse<ViolationResponse>> getViolation(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success(
                violationService.getViolation(id, principal)));
    }

    // ── PATCH /api/v1/violations/{id}/status ─────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR')")
    @Operation(summary = "Update violation workflow status",
               description = "Valid transitions: OPEN → IN_REVIEW → RESOLVED | FALSE_POSITIVE. "
                           + "A `note` is required when resolving or marking as false positive.")
    public ResponseEntity<ApiResponse<ViolationResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateViolationStatusRequest request,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success("Violation status updated",
                violationService.updateStatus(id, request, principal)));
    }

    // ── GET /api/v1/violations/summary ───────────────────────────

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get aggregated violation counts for the organisation dashboard")
    public ResponseEntity<ApiResponse<ViolationSummaryResponse>> getSummary(
            @RequestParam(required = false) UUID organizationId,
            @CurrentUser CompliancePrincipal principal) {

        UUID orgId = organizationId != null ? organizationId : principal.getOrganizationId();
        return ResponseEntity.ok(ApiResponse.success(
                violationService.getSummary(orgId, principal)));
    }
}
