package com.company.compliance.dto.request;

import com.company.compliance.domain.enums.Severity;
import com.company.compliance.domain.enums.ViolationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Violation list filter parameters")
public class ViolationFilterRequest {

    private UUID policyId;
    private UUID userId;
    private List<Severity> severities;
    private List<ViolationStatus> statuses;
    private String framework;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime detectedFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime detectedTo;

    @Schema(description = "Only return violations affecting the risk score")
    private Boolean affectsRiskScore;

    @Min(0) private int page = 0;
    @Min(1) @Max(100) private int size = 20;
    private String sortBy = "detectedAt";
    private String sortDir = "DESC";
}
