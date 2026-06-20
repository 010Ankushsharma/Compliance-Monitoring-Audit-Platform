package com.company.compliance.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Supported regulatory and security compliance frameworks.
 *
 * <p>Each constant carries metadata used in report generation, policy seeding,
 * and UI labelling: a short code, a display name, the governing body, and
 * the industry/scope where the framework typically applies.
 */
@Getter
@RequiredArgsConstructor
public enum RegulatoryFramework {

    ISO_27001(
            "ISO_27001",
            "ISO/IEC 27001:2022",
            "International Organization for Standardization (ISO)",
            "Information security management system (ISMS) — all industries",
            "https://www.iso.org/standard/82875.html",
            ReportTemplate.ISO27001_GAP_ANALYSIS,
            true
    ),

    SOC2(
            "SOC2",
            "SOC 2 Type II",
            "American Institute of CPAs (AICPA)",
            "SaaS / cloud service providers — Trust Services Criteria",
            "https://www.aicpa-cima.com/resources/landing/system-and-organization-controls-soc-suite-of-services",
            ReportTemplate.SOC2_TYPE2,
            true
    ),

    GDPR(
            "GDPR",
            "General Data Protection Regulation",
            "European Union",
            "Any organisation processing personal data of EU residents",
            "https://gdpr-info.eu/",
            ReportTemplate.GDPR_COMPLIANCE,
            true
    ),

    HIPAA(
            "HIPAA",
            "Health Insurance Portability and Accountability Act",
            "U.S. Department of Health & Human Services (HHS)",
            "U.S. healthcare providers, health plans, and business associates",
            "https://www.hhs.gov/hipaa/index.html",
            ReportTemplate.HIPAA_SECURITY,
            true
    ),

    PCI_DSS(
            "PCI_DSS",
            "PCI DSS v4.0",
            "PCI Security Standards Council (PCI SSC)",
            "Any entity that stores, processes, or transmits cardholder data",
            "https://www.pcisecuritystandards.org/",
            ReportTemplate.PCI_DSS_ASSESSMENT,
            true
    ),

    NIST(
            "NIST",
            "NIST Cybersecurity Framework 2.0",
            "National Institute of Standards and Technology (NIST)",
            "U.S. federal agencies and critical infrastructure — all sectors",
            "https://www.nist.gov/cyberframework",
            ReportTemplate.NIST_CSF_ASSESSMENT,
            true
    ),

    CUSTOM(
            "CUSTOM",
            "Custom Framework",
            "Internal / Organisation-defined",
            "Organisation-specific compliance controls",
            null,
            ReportTemplate.CUSTOM,
            false // custom frameworks do not have a pre-built report template
    );

    /** Canonical string value persisted to the database and serialised in JSON. */
    @JsonValue
    private final String value;

    /** Full display name shown in reports and UI. */
    private final String displayName;

    /** Governing body or issuing organisation. */
    private final String governingBody;

    /** Typical industry or scope of applicability. */
    private final String applicability;

    /** Official URL for the framework specification (nullable for CUSTOM). */
    private final String specificationUrl;

    /** Default report template identifier used when generating framework evidence reports. */
    private final ReportTemplate defaultReportTemplate;

    /**
     * {@code true} if a pre-built policy seed and report template exist for
     * this framework out-of-the-box.
     */
    private final boolean hasPrebuiltTemplate;

    // ── Nested enum: Report Templates ────────────────────────────

    /**
     * Pre-built report template identifiers.
     * Values match the {@code template_id} column in the {@code reports} table.
     */
    public enum ReportTemplate {
        ISO27001_GAP_ANALYSIS   ("iso27001-gap-analysis"),
        SOC2_TYPE2              ("soc2-type2"),
        GDPR_COMPLIANCE         ("gdpr-compliance"),
        HIPAA_SECURITY          ("hipaa-security-rule"),
        PCI_DSS_ASSESSMENT      ("pci-dss-v4-assessment"),
        NIST_CSF_ASSESSMENT     ("nist-csf-2-assessment"),
        CUSTOM                  ("custom");

        @Getter
        @JsonValue
        private final String templateId;

        ReportTemplate(String templateId) {
            this.templateId = templateId;
        }
    }

    // ── Factory ──────────────────────────────────────────────────

    @JsonCreator
    public static RegulatoryFramework fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("RegulatoryFramework value must not be null");
        }
        for (RegulatoryFramework f : values()) {
            if (f.value.equalsIgnoreCase(value.trim())) {
                return f;
            }
        }
        throw new IllegalArgumentException(
                "Unknown RegulatoryFramework value: '" + value + "'. "
                        + "Accepted values: " + Arrays.toString(values()));
    }

    // ── Convenience ──────────────────────────────────────────────

    /**
     * Returns all frameworks that have a pre-built report template.
     * Used by the report template listing endpoint.
     */
    public static List<RegulatoryFramework> prebuiltFrameworks() {
        return Arrays.stream(values())
                .filter(RegulatoryFramework::isHasPrebuiltTemplate)
                .toList();
    }

    /**
     * Returns {@code true} if this framework mandates breach notification
     * within a defined regulatory window.
     */
    public boolean hasBreachNotificationRequirement() {
        return this == GDPR || this == HIPAA || this == PCI_DSS;
    }

    /**
     * Returns the maximum breach notification window in hours,
     * or -1 if the framework does not specify one.
     */
    public int breachNotificationWindowHours() {
        return switch (this) {
            case GDPR    -> 72;
            case HIPAA   -> 60 * 24; // 60 calendar days converted to hours
            case PCI_DSS -> 24;
            default      -> -1;
        };
    }

    @Override
    public String toString() {
        return value;
    }
}
