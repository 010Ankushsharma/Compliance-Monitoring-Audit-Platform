package com.company.compliance.mapper;

import com.company.compliance.domain.entity.Report;
import com.company.compliance.dto.response.ReportResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for {@link Report} → response DTO.
 *
 * <p>The {@code downloadUrl} field is not sourced from the entity —
 * it is set by the service after mapping via {@code response.setDownloadUrl(url)}.
 *
 * <p>File: {@code src/main/java/com/company/compliance/mapper/ReportMapper.java}
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ReportMapper {

    @Mapping(source = "organization.id",       target = "organizationId")
    @Mapping(source = "generatedBy.id",        target = "generatedById")
    @Mapping(source = "generatedBy.fullName",  target = "generatedByName")
    @Mapping(target = "downloadUrl",           ignore = true) // set post-mapping by service
    ReportResponse toResponse(Report report);

    List<ReportResponse> toResponseList(List<Report> reports);
}
