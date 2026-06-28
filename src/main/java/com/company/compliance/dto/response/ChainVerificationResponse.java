package com.company.compliance.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of audit log hash-chain integrity verification")
public class ChainVerificationResponse {

    @Schema(description = "True if the entire chain is intact — no tampering detected")
    private boolean intact;

    @Schema(description = "Total number of audit log entries verified")
    private long totalEntries;

    @Schema(description = "Number of broken links detected (0 = clean)")
    private int brokenLinks;

    @Schema(description = "IDs of entries where the chain is broken")
    private List<UUID> brokenEntryIds;

    @Schema(description = "Timestamp of the oldest entry verified")
    private OffsetDateTime verifiedFrom;

    @Schema(description = "Timestamp of the most recent entry verified")
    private OffsetDateTime verifiedTo;

    @Schema(description = "When this verification was performed")
    @Builder.Default
    private OffsetDateTime verifiedAt = OffsetDateTime.now();

    @Schema(description = "Hash algorithm used", example = "SHA-256")
    @Builder.Default
    private String algorithm = "SHA-256";
}
