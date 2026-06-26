package com.company.compliance.mapper;

import com.company.compliance.domain.entity.Alert;
import com.company.compliance.dto.response.AlertResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for {@link Alert} → response DTO.
 *
 * <p>File: {@code src/main/java/com/company/compliance/mapper/AlertMapper.java}
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AlertMapper {

    @Mapping(source = "organization.id",         target = "organizationId")
    @Mapping(source = "violation.id",            target = "violationId")
    @Mapping(source = "acknowledgedBy.id",       target = "acknowledgedById")
    @Mapping(source = "acknowledgedBy.fullName", target = "acknowledgedByName")
    @Mapping(source = "resolvedBy.id",           target = "resolvedById")
    AlertResponse toResponse(Alert alert);

    List<AlertResponse> toResponseList(List<Alert> alerts);
}
