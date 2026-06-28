package com.company.compliance.dto.request;

import com.company.compliance.domain.enums.ViolationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Update a violation's workflow status")
public class UpdateViolationStatusRequest {

    @NotNull(message = "New status is required")
    @Schema(example = "RESOLVED")
    private ViolationStatus status;

    @Size(max = 5000, message = "Note must not exceed 5000 characters")
    @Schema(description = "Required when resolving or marking as false positive",
            example = "MFA has been enforced via policy update. Verified by security team.")
    private String note;
}
