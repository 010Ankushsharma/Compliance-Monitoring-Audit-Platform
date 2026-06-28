package com.company.compliance.dto.request;

import com.company.compliance.domain.enums.Severity;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateAlertRequest {

    private UUID violationId;

    @NotNull(message = "Severity is required")
    private Severity severity;

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @Size(max = 100)
    private String source;

    private String dedupKey;
}
