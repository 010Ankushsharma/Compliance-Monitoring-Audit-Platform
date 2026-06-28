package com.company.compliance.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Lifecycle status of a {@code Policy}.
 *
 * <p>Valid state transitions:
 * <pre>
 *   DRAFT ──► ACTIVE ──► INACTIVE ──► ARCHIVED
 *     │                      │
 *     └──────────────────────┘  (re-draft / edit)
 * </pre>
 *
 * <p>Only {@code ACTIVE} policies are included in violation detection runs.
 */
@Getter
@RequiredArgsConstructor
public enum PolicyStatus {

    DRAFT   ("DRAFT",    false, "Policy is being authored and has not yet been published"),
    ACTIVE  ("ACTIVE",   true,  "Policy is published and actively evaluated"),
    INACTIVE("INACTIVE", false, "Policy is paused; violations are not generated"),
    ARCHIVED("ARCHIVED", false, "Policy is retired and read-only; kept for audit history");

    /** Canonical string value persisted to the database and serialised in JSON. */
    @JsonValue
    private final String value;

    /**
     * {@code true} when the policy should be included in scheduled evaluation runs
     * and violation detection.
     */
    private final boolean evaluable;

    /** Human-readable description. */
    private final String description;

    // ── Factory ──────────────────────────────────────────────────

    @JsonCreator
    public static PolicyStatus fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("PolicyStatus value must not be null");
        }
        for (PolicyStatus s : values()) {
            if (s.value.equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException(
                "Unknown PolicyStatus value: '" + value + "'. "
                        + "Accepted values: DRAFT, ACTIVE, INACTIVE, ARCHIVED");
    }

    // ── Transition guards ────────────────────────────────────────

    /**
     * Returns {@code true} if transitioning from this status to {@code target}
     * is a legal state-machine move.
     */
    public boolean canTransitionTo(PolicyStatus target) {
        return switch (this) {
            case DRAFT    -> target == ACTIVE;
            case ACTIVE   -> target == INACTIVE || target == ARCHIVED;
            case INACTIVE -> target == ACTIVE   || target == ARCHIVED || target == DRAFT;
            case ARCHIVED -> false; // terminal state
        };
    }

    /** Returns {@code true} if this is a terminal state (no further transitions allowed). */
    public boolean isTerminal() {
        return this == ARCHIVED;
    }

    @Override
    public String toString() {
        return value;
    }
}
