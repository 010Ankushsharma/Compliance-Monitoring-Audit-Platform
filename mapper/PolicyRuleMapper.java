package com.company.compliance.mapper;

import com.company.compliance.domain.entity.PolicyRule;
import com.company.compliance.dto.request.CreatePolicyRuleRequest;
import com.company.compliance.dto.response.PolicyRuleResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for {@link PolicyRule} ↔ DTOs.
 *
 * <p>File: {@code src/main/java/com/company/compliance/mapper/PolicyRuleMapper.java}
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PolicyRuleMapper {

    // ── entity → response ────────────────────────────────────────

    @Mapping(source = "policy.id", target = "policyId")
    PolicyRuleResponse toResponse(PolicyRule rule);

    List<PolicyRuleResponse> toResponseList(List<PolicyRule> rules);

    // ── create request → entity ───────────────────────────────────

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "policy",     ignore = true)   // set by PolicyService.addRule()
    @Mapping(target = "active",     constant = "true")
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    PolicyRule fromCreateRequest(CreatePolicyRuleRequest request);
}
