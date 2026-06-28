package com.company.compliance.mapper;

import com.company.compliance.domain.entity.ComplianceViolation;
import com.company.compliance.dto.response.ViolationResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for {@link ComplianceViolation} → response DTO.
 *
 * <p>Violations are created by the violation detection engine (not via API),
 * so there is no create-request → entity mapping here.
 *
 * <p>File: {@code src/main/java/com/company/compliance/mapper/ViolationMapper.java}
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ViolationMapper {

    @Mapping(source = "organization.id",               target = "organizationId")
    @Mapping(source = "policy.id",                     target = "policyId")
    @Mapping(source = "policy.name",                   target = "policyName")
    @Mapping(source = "policy.framework",              target = "framework",
             qualifiedByName = "frameworkToString")
    @Mapping(source = "policyRule.id",                 target = "policyRuleId")
    @Mapping(source = "policyRule.name",               target = "policyRuleName")
    @Mapping(source = "user.id",                       target = "userId")
    @Mapping(source = "user.email",                    target = "userEmail")
    @Mapping(source = "acknowledgedBy.id",             target = "acknowledgedById")
    @Mapping(source = "acknowledgedBy.fullName",       target = "acknowledgedByName")
    @Mapping(source = "resolvedBy.id",                 target = "resolvedById")
    @Mapping(source = "resolvedBy.fullName",           target = "resolvedByName")
    @Mapping(target = "affectsRiskScore",
             expression = "java(violation.affectsRiskScore())")
    ViolationResponse toResponse(ComplianceViolation violation);

    List<ViolationResponse> toResponseList(List<ComplianceViolation> violations);

    // ── Named conversion method ───────────────────────────────────

    @Named("frameworkToString")
    default String frameworkToString(
            com.company.compliance.domain.enums.RegulatoryFramework framework) {
        return framework != null ? framework.getValue() : null;
    }
}
