package com.company.compliance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Organisation profile response DTO.
 *
 * <p>File: {@code src/main/java/com/company/compliance/dto/response/OrganizationResponse.java}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Organisation profile response")
public class OrganizationResponse {

    private UUID            id;
    private String          name;
    private String          industry;
    private String          country;
    private List<String>    regulatoryFrameworks;
    private String          contactEmail;
    private boolean         active;
    private BigDecimal      overallRiskScore;
    private OffsetDateTime  riskLastUpdated;
    private OffsetDateTime  createdAt;
    private OffsetDateTime  updatedAt;
}
