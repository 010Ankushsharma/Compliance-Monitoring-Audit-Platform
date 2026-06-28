package com.company.compliance.mapper;

import com.company.compliance.domain.entity.Organization;
import com.company.compliance.dto.response.OrganizationResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for {@link Organization} → response DTO.
 *
 * <p>File: {@code src/main/java/com/company/compliance/mapper/OrganizationMapper.java}
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface OrganizationMapper {

    @Mapping(target = "active", source = "active")
    OrganizationResponse toResponse(Organization organization);

    List<OrganizationResponse> toResponseList(List<Organization> organizations);
}
