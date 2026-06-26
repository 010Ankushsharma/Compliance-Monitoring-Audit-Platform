package com.company.compliance.mapper;

import com.company.compliance.domain.entity.AuditLog;
import com.company.compliance.dto.response.AuditLogResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for {@link AuditLog} → response DTO.
 *
 * <p>AuditLog is write-once (immutable), so there is intentionally
 * no mapping from DTO → entity. Creation is handled exclusively by
 * {@link com.company.compliance.service.AuditLogService#persistAuditLog}.
 *
 * <p>File: {@code src/main/java/com/company/compliance/mapper/AuditLogMapper.java}
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AuditLogMapper {

    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "user.id",         target = "userId")
    AuditLogResponse toResponse(AuditLog auditLog);

    List<AuditLogResponse> toResponseList(List<AuditLog> auditLogs);
}
