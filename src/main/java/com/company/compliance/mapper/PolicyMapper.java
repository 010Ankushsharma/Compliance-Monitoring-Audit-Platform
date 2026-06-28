package com.company.compliance.mapper;

import com.company.compliance.domain.entity.Policy;
import com.company.compliance.domain.entity.PolicyRule;
import com.company.compliance.dto.request.CreatePolicyRequest;
import com.company.compliance.dto.request.CreatePolicyRuleRequest;
import com.company.compliance.dto.response.PolicyResponse;
import com.company.compliance.dto.response.PolicyRuleResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct compile-time mapper for {@link Policy} ↔ DTOs.
 *
 * <p>Spring-managed bean (componentModel = "spring") injected into services.
 * All unmapped target properties fail compilation ({@code unmappedTargetPolicy = ERROR}).
 *
 * <p>File: {@code src/main/java/com/company/compliance/mapper/PolicyMapper.java}
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = { PolicyRuleMapper.class }
)
public interface PolicyMapper {

    // ── Policy: entity → response ─────────────────────────────────

    @Mapping(source = "organization.id",          target = "organizationId")
    @Mapping(source = "owner.id",                 target = "ownerId")
    @Mapping(source = "owner.fullName",           target = "ownerName")
    @Mapping(source = "createdBy.id",             target = "createdById")
    @Mapping(source = "createdBy.fullName",       target = "createdByName")
    @Mapping(target = "ruleCount",                expression = "java(policy.getRules().size())")
    @Mapping(target = "evaluable",                expression = "java(policy.isEvaluable())")
    @Mapping(target = "expired",                  expression = "java(policy.isExpired())")
    PolicyResponse toResponse(Policy policy);

    List<PolicyResponse> toResponseList(List<Policy> policies);

    // ── Policy: create request → entity (partial — org/user set by service) ──

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "organization",     ignore = true)
    @Mapping(target = "owner",            ignore = true)
    @Mapping(target = "createdBy",        ignore = true)
    @Mapping(target = "status",           constant = "DRAFT")
    @Mapping(target = "version",          constant = "1")
    @Mapping(target = "rules",            ignore = true)  // rules added separately
    @Mapping(target = "createdAt",        ignore = true)
    @Mapping(target = "updatedAt",        ignore = true)
    @Mapping(target = "deletedAt",        ignore = true)
    @Mapping(target = "lastEvaluatedAt",  ignore = true)
    @Mapping(target = "nextEvaluatedAt",  ignore = true)
    @Mapping(source = "evaluationSchedule", target = "evaluationSchedule",
             defaultValue = "0 0 * * *")
    Policy fromCreateRequest(CreatePolicyRequest request);

    // ── Policy: update request → existing entity (PATCH semantics) ─

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "organization",     ignore = true)
    @Mapping(target = "framework",        ignore = true)  // framework cannot be changed post-creation
    @Mapping(target = "owner",            ignore = true)  // owner resolved by service
    @Mapping(target = "createdBy",        ignore = true)
    @Mapping(target = "rules",            ignore = true)
    @Mapping(target = "version",          ignore = true)
    @Mapping(target = "createdAt",        ignore = true)
    @Mapping(target = "updatedAt",        ignore = true)
    @Mapping(target = "deletedAt",        ignore = true)
    @Mapping(target = "lastEvaluatedAt",  ignore = true)
    @Mapping(target = "nextEvaluatedAt",  ignore = true)
    void updateFromRequest(
            @MappingTarget Policy policy,
            com.company.compliance.dto.request.UpdatePolicyRequest request);
}
