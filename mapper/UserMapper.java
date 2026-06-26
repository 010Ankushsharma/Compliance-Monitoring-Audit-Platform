package com.company.compliance.mapper;

import com.company.compliance.domain.entity.User;
import com.company.compliance.dto.request.CreateUserRequest;
import com.company.compliance.dto.response.UserResponse;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for {@link User} ↔ DTOs.
 *
 * <p><strong>Security note:</strong> {@code passwordHash} and {@code mfaSecret}
 * are explicitly ignored in every outbound mapping — they must never appear
 * in API responses.
 *
 * <p>File: {@code src/main/java/com/company/compliance/mapper/UserMapper.java}
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    // ── entity → response ─────────────────────────────────────────

    @Mapping(source = "organization.id",   target = "organizationId")
    @Mapping(source = "organization.name", target = "organizationName")
    // NEVER map passwordHash, mfaSecret to the response
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    // ── create request → entity ───────────────────────────────────

    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "organization",   ignore = true)   // set by UserService
    @Mapping(target = "passwordHash",   ignore = true)   // hashed by UserService
    @Mapping(target = "mfaEnabled",     constant = "false")
    @Mapping(target = "mfaSecret",      ignore = true)
    @Mapping(target = "active",         constant = "true")
    @Mapping(target = "lastLoginAt",    ignore = true)
    @Mapping(target = "failedLogins",   ignore = true)
    @Mapping(target = "lockedUntil",    ignore = true)
    @Mapping(target = "createdAt",      ignore = true)
    @Mapping(target = "updatedAt",      ignore = true)
    @Mapping(target = "deletedAt",      ignore = true)
    User fromCreateRequest(CreateUserRequest request);

    // ── update request → existing entity (PATCH semantics) ────────

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "email",        ignore = true)    // email changes require re-verification
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "mfaEnabled",   ignore = true)
    @Mapping(target = "mfaSecret",    ignore = true)
    @Mapping(target = "lastLoginAt",  ignore = true)
    @Mapping(target = "failedLogins", ignore = true)
    @Mapping(target = "lockedUntil",  ignore = true)
    @Mapping(target = "createdAt",    ignore = true)
    @Mapping(target = "updatedAt",    ignore = true)
    @Mapping(target = "deletedAt",    ignore = true)
    void updateFromRequest(
            @MappingTarget User user,
            com.company.compliance.dto.request.UpdateUserRequest request);
}
