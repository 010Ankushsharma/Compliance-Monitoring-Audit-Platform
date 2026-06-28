package com.company.compliance.service;

import com.company.compliance.annotation.Auditable;
import com.company.compliance.domain.entity.Organization;
import com.company.compliance.domain.entity.Policy;
import com.company.compliance.domain.entity.PolicyRule;
import com.company.compliance.domain.entity.User;
import com.company.compliance.domain.enums.PolicyStatus;
import com.company.compliance.domain.enums.RegulatoryFramework;
import com.company.compliance.domain.enums.Severity;
import com.company.compliance.dto.request.CreatePolicyRequest;
import com.company.compliance.dto.request.CreatePolicyRuleRequest;
import com.company.compliance.dto.request.UpdatePolicyRequest;
import com.company.compliance.dto.response.PolicyResponse;
import com.company.compliance.dto.common.PageResponse;
import com.company.compliance.exception.ConflictException;
import com.company.compliance.exception.ResourceNotFoundException;
import com.company.compliance.exception.InvalidStateTransitionException;
import com.company.compliance.repository.OrganizationRepository;
import com.company.compliance.repository.PolicyRepository;
import com.company.compliance.repository.UserRepository;
import com.company.compliance.security.CompliancePrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Policy lifecycle management service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD for policies and their rules</li>
 *   <li>Status transition validation via {@link PolicyStatus#canTransitionTo}</li>
 *   <li>Multi-tenant isolation — users can only access their own organisation's policies</li>
 *   <li>Cache invalidation on every write</li>
 * </ul>
 *
 * <p>File: {@code src/main/java/com/company/compliance/service/PolicyService.java}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository       policyRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository         userRepository;

    // ── Create ────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    @Auditable(action = "POLICY_CREATED", resourceType = "POLICY")
    public PolicyResponse createPolicy(CreatePolicyRequest req,
                                       CompliancePrincipal principal) {
        Organization org = resolveOrganization(principal);

        if (policyRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull(
                req.getName(), org.getId())) {
            throw new ConflictException(
                    "A policy named '" + req.getName() + "' already exists in this organisation");
        }

        User createdBy = resolveUser(principal.getUserId());
        User owner = req.getOwnerId() != null ? resolveUser(req.getOwnerId()) : createdBy;

        Policy policy = Policy.builder()
                .organization(org)
                .name(req.getName())
                .description(req.getDescription())
                .framework(req.getFramework())
                .severity(req.getSeverity())
                .status(PolicyStatus.DRAFT)
                .effectiveDate(req.getEffectiveDate())
                .expiryDate(req.getExpiryDate())
                .owner(owner)
                .tags(req.getTags() != null ? req.getTags() : List.of())
                .evaluationSchedule(req.getEvaluationSchedule() != null
                        ? req.getEvaluationSchedule() : "0 0 * * *")
                .createdBy(createdBy)
                .build();

        // Attach initial rules
        if (req.getRules() != null) {
            req.getRules().forEach(ruleReq -> policy.addRule(buildRule(ruleReq, policy)));
        }

        Policy saved = policyRepository.save(policy);
        log.info("Policy '{}' created [id={}] by user {}", saved.getName(), saved.getId(),
                principal.getUserId());
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Cacheable(value = "policies", key = "#policyId + ':' + #principal.organizationId")
    public PolicyResponse getPolicy(UUID policyId, CompliancePrincipal principal) {
        Policy policy = resolvePolicyForOrg(policyId, principal);
        return toResponse(policy);
    }

    @Transactional(readOnly = true)
    public PageResponse<PolicyResponse> listPolicies(UUID organizationId,
                                                     PolicyStatus status,
                                                     RegulatoryFramework framework,
                                                     Severity severity,
                                                     int page, int size,
                                                     CompliancePrincipal principal) {
        assertCanAccess(organizationId, principal);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var policies = policyRepository.findWithFilters(
                organizationId, status, framework, severity, pageable);
        return PageResponse.from(policies.map(this::toResponse));
    }

    // ── Update ────────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    @Auditable(action = "POLICY_UPDATED", resourceType = "POLICY", resourceIdArg = "policyId")
    public PolicyResponse updatePolicy(UUID policyId,
                                       UpdatePolicyRequest req,
                                       CompliancePrincipal principal) {
        Policy policy = resolvePolicyForOrg(policyId, principal);

        if (policy.getStatus() == PolicyStatus.ARCHIVED) {
            throw new InvalidStateTransitionException("Archived policies cannot be modified");
        }

        // Check name uniqueness if changing name
        if (req.getName() != null && !req.getName().equals(policy.getName())) {
            if (policyRepository.existsByNameAndOrganizationIdAndIdNotAndDeletedAtIsNull(
                    req.getName(), policy.getOrganization().getId(), policyId)) {
                throw new ConflictException("Policy name '" + req.getName() + "' already exists");
            }
        }

        // Apply status transition if requested
        if (req.getStatus() != null && req.getStatus() != policy.getStatus()) {
            if (!policy.getStatus().canTransitionTo(req.getStatus())) {
                throw new InvalidStateTransitionException(
                        "Cannot transition policy from " + policy.getStatus()
                        + " to " + req.getStatus());
            }
            policy.setStatus(req.getStatus());
        }

        if (req.getName()               != null) policy.setName(req.getName());
        if (req.getDescription()        != null) policy.setDescription(req.getDescription());
        if (req.getSeverity()           != null) policy.setSeverity(req.getSeverity());
        if (req.getEffectiveDate()      != null) policy.setEffectiveDate(req.getEffectiveDate());
        if (req.getExpiryDate()         != null) policy.setExpiryDate(req.getExpiryDate());
        if (req.getEvaluationSchedule() != null) policy.setEvaluationSchedule(req.getEvaluationSchedule());
        if (req.getTags()               != null) policy.setTags(req.getTags());
        if (req.getOwnerId()            != null) policy.setOwner(resolveUser(req.getOwnerId()));

        policy.bumpVersion();
        Policy saved = policyRepository.save(policy);
        log.info("Policy '{}' updated [id={}] by {}", saved.getName(), saved.getId(),
                principal.getUserId());
        return toResponse(saved);
    }

    // ── Delete (soft) ─────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    @Auditable(action = "POLICY_DELETED", resourceType = "POLICY", resourceIdArg = "policyId")
    public void deletePolicy(UUID policyId, CompliancePrincipal principal) {
        Policy policy = resolvePolicyForOrg(policyId, principal);
        policy.softDelete();
        policyRepository.save(policy);
        log.info("Policy '{}' soft-deleted [id={}] by {}", policy.getName(), policyId,
                principal.getUserId());
    }

    // ── Rule management ───────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public PolicyResponse addRule(UUID policyId,
                                  CreatePolicyRuleRequest req,
                                  CompliancePrincipal principal) {
        Policy policy = resolvePolicyForOrg(policyId, principal);
        policy.addRule(buildRule(req, policy));
        policy.bumpVersion();
        return toResponse(policyRepository.save(policy));
    }

    @Transactional
    @CacheEvict(value = "policies", allEntries = true)
    public void deleteRule(UUID policyId, UUID ruleId, CompliancePrincipal principal) {
        Policy policy = resolvePolicyForOrg(policyId, principal);
        PolicyRule rule = policy.getRules().stream()
                .filter(r -> r.getId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Rule", ruleId));
        policy.removeRule(rule);
        policy.bumpVersion();
        policyRepository.save(policy);
    }

    // ── Scheduler support ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Policy> findPoliciesDueForEvaluation() {
        return policyRepository.findPoliciesDueForEvaluation(OffsetDateTime.now());
    }

    @Transactional
    public void updateEvaluationTimestamps(UUID policyId,
                                           OffsetDateTime lastEval,
                                           OffsetDateTime nextEval) {
        policyRepository.updateEvaluationTimestamps(policyId, lastEval, nextEval);
    }

    // ── Private helpers ───────────────────────────────────────────

    private Policy resolvePolicyForOrg(UUID policyId, CompliancePrincipal principal) {
        UUID orgId = principal.isSuperAdmin()
                ? policyRepository.findByIdAndDeletedAtIsNull(policyId)
                        .map(p -> p.getOrganization().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Policy", policyId))
                : principal.getOrganizationId();

        return policyRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", policyId));
    }

    private Organization resolveOrganization(CompliancePrincipal principal) {
        return organizationRepository
                .findByIdAndDeletedAtIsNull(principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organisation", principal.getOrganizationId()));
    }

    private User resolveUser(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void assertCanAccess(UUID organizationId, CompliancePrincipal principal) {
        if (!principal.canManage(organizationId)) {
            throw new AccessDeniedException("Access denied to organisation " + organizationId);
        }
    }

    private PolicyRule buildRule(CreatePolicyRuleRequest req, Policy policy) {
        return PolicyRule.builder()
                .policy(policy)
                .name(req.getName())
                .description(req.getDescription())
                .ruleType(req.getRuleType())
                .field(req.getField())
                .operator(req.getOperator())
                .value(req.getValue())
                .gracePeriodDays(req.getGracePeriodDays())
                .evaluationOrder(req.getEvaluationOrder())
                .active(true)
                .build();
    }

    private PolicyResponse toResponse(Policy p) {
        return PolicyResponse.builder()
                .id(p.getId())
                .organizationId(p.getOrganization().getId())
                .name(p.getName())
                .description(p.getDescription())
                .framework(p.getFramework())
                .severity(p.getSeverity())
                .status(p.getStatus())
                .version(p.getVersion())
                .effectiveDate(p.getEffectiveDate())
                .expiryDate(p.getExpiryDate())
                .ownerId(p.getOwner() != null ? p.getOwner().getId() : null)
                .ownerName(p.getOwner() != null ? p.getOwner().getFullName() : null)
                .tags(p.getTags())
                .evaluationSchedule(p.getEvaluationSchedule())
                .lastEvaluatedAt(p.getLastEvaluatedAt())
                .nextEvaluatedAt(p.getNextEvaluatedAt())
                .createdById(p.getCreatedBy().getId())
                .createdByName(p.getCreatedBy().getFullName())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .ruleCount(p.getRules().size())
                .evaluable(p.isEvaluable())
                .expired(p.isExpired())
                .build();
    }
}
