package com.company.compliance.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Severity levels used across policies, violations, and alerts.
 *
 * <p>Ordered from highest to lowest (CRITICAL → INFO). The numeric {@code level}
 * field enables range comparisons:
 * {@code severity.getLevel() >= Severity.HIGH.getLevel()}.
 *
 * <p>Risk-score penalty weights ({@code riskWeight}) feed the
 * {@code RiskScoringService} when computing a policy's compliance score.
 */
@Getter
@RequiredArgsConstructor
public enum Severity {

    CRITICAL("CRITICAL", 5, 40.0, "#D32F2F", "Immediate action required — systemic or regulatory risk"),
    HIGH    ("HIGH",     4, 25.0, "#F57C00", "Significant risk requiring prompt remediation"),
    MEDIUM  ("MEDIUM",   3, 15.0, "#FBC02D", "Moderate risk; address within standard SLA"),
    LOW     ("LOW",      2,  5.0, "#388E3C", "Minor risk; schedule for next maintenance cycle"),
    INFO    ("INFO",     1,  0.0, "#1976D2", "Informational — no scoring impact");

    /** Canonical string value persisted to the database and serialised in JSON. */
    @JsonValue
    private final String value;

    /**
     * Numeric level for ordered comparison.
     * Higher number = more severe.
     */
    private final int level;

    /**
     * Penalty subtracted from a policy's compliance score (0–100)
     * for each open violation at this severity.
     */
    private final double riskWeight;

    /** Hex colour for UI badges and alert indicators. */
    private final String hexColour;

    /** Human-readable description shown in API responses and reports. */
    private final String description;

    // ── Factory ──────────────────────────────────────────────────

    @JsonCreator
    public static Severity fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Severity value must not be null");
        }
        for (Severity s : values()) {
            if (s.value.equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException(
                "Unknown Severity value: '" + value + "'. "
                        + "Accepted values: CRITICAL, HIGH, MEDIUM, LOW, INFO");
    }

    // ── Comparison helpers ────────────────────────────────────────

    /** Returns {@code true} if this severity is at least as severe as {@code other}. */
    public boolean isAtLeast(Severity other) {
        return this.level >= other.level;
    }

    /** Returns {@code true} if this severity is more severe than {@code other}. */
    public boolean isHigherThan(Severity other) {
        return this.level > other.level;
    }

    /** Returns {@code true} if this severity warrants an immediate page/alert. */
    public boolean requiresImmediateAction() {
        return this.level >= CRITICAL.level;
    }

    /** Returns {@code true} if this severity should trigger out-of-hours notifications. */
    public boolean isOnCallEscalation() {
        return this.level >= HIGH.level;
    }

    @Override
    public String toString() {
        return value;
    }
}
