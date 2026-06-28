package com.company.compliance.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 255)
    private String name;

    @Size(max = 100)
    private String industry;

    @Size(max = 100)
    private String country;

    private List<String> regulatoryFrameworks;

    @Email(message = "Must be a valid email")
    private String contactEmail;
}
