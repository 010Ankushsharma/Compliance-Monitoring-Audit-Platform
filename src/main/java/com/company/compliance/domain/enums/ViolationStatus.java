package com.company.compliance.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Workflow status of a {@code ComplianceViolation}.
 *
 * <p>Valid state transitions:
 * <pre>
 *                    ┌──────────────────────────────────┐
 *                    ▼                                  │
 *   OPEN ──► IN_REVIEW ──► RESOLVED                    │
 *     │          │                                      │
 *     │          └──► FALSE_POSITIVE ──────────────────┘
 *     │
 *     └──► SUPPRESSED  (admin action, e.g. scheduled maintenance)
 * </pre>
 *
 * <p>Only {@code OPEN} and {@code IN_REVIEW} violations contribute to the
 * risk score penalty computation.
 */
@Getter
@RequiredArgsConstructor
public enum ViolationStatus {

    OPEN          ("OPEN",           true,  "Violation detected and awaiting triage"),
    IN_REVIEW     ("IN_REVIEW",      true,  "Violation is being actively investigated"),
    RESOLVED      ("RESOLVED",       false, "Violation has been remediated and verified"),
    FALSE_POSITIVE("FALSE_POSITIVE", false, "Violation was determined to be a detection error"),
    SUPPRESSED    ("SUPPRESSED",     false, "Violation is intentionally suppressed (e.g. planned maintenance)");

    /** Canonical string value persisted to the database and serialised in JSON. */
    @JsonValue
    private final String value;

    /**
     * {@code true} when this status means the violation is still open
     * and should contribute to risk scores and dashboard counts.
     */
    private final boolean affectsRiskScore;

    /** Human-readable description. */
    private final String description;

    // ── Factory ──────────────────────────────────────────────────

    @JsonCreator
    public static ViolationStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("ViolationStatus value must not be null");
        }
        for (ViolationStatus s : values()) {
            if (s.value.equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException(
                "Unknown ViolationStatus value: '" + value + "'. "
                        + "Accepted values: OPEN, IN_REVIEW, RESOLVED, FALSE_POSITIVE, SUPPRESSED");
    }

    // ── Transition guards ────────────────────────────────────────

    /**
     * Returns {@code true} if transitioning from this status to {@code target}
     * is a legal workflow move.
     */
    public boolean canTransitionTo(ViolationStatus target) {
        return switch (this) {
            case OPEN          -> target == IN_REVIEW || target == RESOLVED
                                  || target == FALSE_POSITIVE || target == SUPPRESSED;
            case IN_REVIEW     -> target == RESOLVED || target == FALSE_POSITIVE
                                  || target == OPEN;
            case RESOLVED      -> target == OPEN; // re-open if violation recurs
            case FALSE_POSITIVE -> target == OPEN; // re-open if diagnosis was wrong
            case SUPPRESSED    -> target == OPEN; // unsuppress when maintenance ends
        };
    }

    /** Returns {@code true} if the violation is in a closed/terminal workflow state. */
    public boolean isClosed() {
        return this == RESOLVED || this == FALSE_POSITIVE;
    }

    /** Returns {@code true} if the violation is currently active (open or under review). */
    public boolean isActive() {
        return this == OPEN || this == IN_REVIEW;
    }

    @Override
    public String toString() {
        return value;
    }
}
