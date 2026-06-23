package com.company.compliance.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Kafka event payload published to {@code compliance.report-requests}.
 *
 * <p>File: {@code src/main/java/com/company/compliance/event/ReportRequestEvent.java}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportRequestEvent {
    private UUID      reportId;
    private UUID      organizationId;
    private String    templateId;
    private String    format;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private boolean   includeEvidence;
}
