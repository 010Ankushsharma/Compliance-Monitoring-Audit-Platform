package com.company.compliance.controller;

import com.company.compliance.dto.common.ApiResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.dto.request.CreatePolicyRequest;
import com.company.compliance.dto.request.CreatePolicyRuleRequest;
import com.company.compliance.dto.request.UpdatePolicyRequest;
import com.company.compliance.dto.response.PolicyResponse;
import com.company.compliance.domain.enums.PolicyStatus;
import com.company.compliance.domain.enums.RegulatoryFramework;
import com.company.compliance.domain.enums.Severity;
import com.company.compliance.security.CompliancePrincipal;
import com.company.compliance.security.CurrentUser;
import com.company.compliance.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Policy management REST controller.
 *
 * <p>File: {@code src/main/java/com/company/compliance/controller/PolicyController.java}
 */
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policies", description = "Compliance policy lifecycle management")
public class PolicyController {

    private final PolicyService policyService;

    // ── POST /api/v1/policies ─────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Create a new compliance policy")
    public ResponseEntity<ApiResponse<PolicyResponse>> createPolicy(
            @Valid @RequestBody CreatePolicyRequest request,
            @CurrentUser CompliancePrincipal principal) {

        PolicyResponse policy = policyService.createPolicy(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Policy created", policy));
    }

    // ── GET /api/v1/policies/{id} ─────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a policy by ID")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicy(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(
                ApiResponse.success(policyService.getPolicy(id, principal)));
    }

    // ── GET /api/v1/policies ──────────────────────────────────────

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List policies for the authenticated organisation with optional filters")
    public ResponseEntity<ApiResponse<PageResponse<PolicyResponse>>> listPolicies(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) PolicyStatus status,
            @RequestParam(required = false) RegulatoryFramework framework,
            @RequestParam(required = false) Severity severity,
            @RequestParam(defaultValue = "0")  @Parameter(description = "Page number (0-based)")  int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size (max 100)") int size,
            @CurrentUser CompliancePrincipal principal) {

        UUID orgId = organizationId != null ? organizationId : principal.getOrganizationId();
        return ResponseEntity.ok(ApiResponse.success(
                policyService.listPolicies(orgId, status, framework, severity,
                        page, size, principal)));
    }

    // ── PUT /api/v1/policies/{id} ─────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Update a policy (all fields optional)")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicy(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePolicyRequest request,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.ok(ApiResponse.success("Policy updated",
                policyService.updatePolicy(id, request, principal)));
    }

    // ── DELETE /api/v1/policies/{id} ──────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Soft-delete a policy (archives it)")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(
            @PathVariable UUID id,
            @CurrentUser CompliancePrincipal principal) {

        policyService.deletePolicy(id, principal);
        return ResponseEntity.ok(ApiResponse.success("Policy deleted"));
    }

    // ── POST /api/v1/policies/{id}/rules ─────────────────────────

    @PostMapping("/{id}/rules")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Add a rule to an existing policy")
    public ResponseEntity<ApiResponse<PolicyResponse>> addRule(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePolicyRuleRequest request,
            @CurrentUser CompliancePrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Rule added",
                        policyService.addRule(id, request, principal)));
    }

    // ── DELETE /api/v1/policies/{id}/rules/{ruleId} ───────────────

    @DeleteMapping("/{id}/rules/{ruleId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','COMPLIANCE_OFFICER')")
    @Operation(summary = "Remove a rule from a policy")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable UUID id,
            @PathVariable UUID ruleId,
            @CurrentUser CompliancePrincipal principal) {

        policyService.deleteRule(id, ruleId, principal);
        return ResponseEntity.ok(ApiResponse.success("Rule deleted"));
    }
}
