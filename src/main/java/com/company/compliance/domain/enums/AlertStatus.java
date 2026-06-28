package com.company.compliance.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Set;

/**
 * Lifecycle status of an {@code Alert}.
 *
 * <p>Valid transitions:
 *
 * <pre>
 *   OPEN ──► ACKNOWLEDGED ──► RESOLVED
 *     │
 *     └──► SUPPRESSED ──► OPEN  (suppression can be lifted)
 * </pre>
 */
public enum AlertStatus {

  /** Alert has been raised and not yet actioned. */
  OPEN("Open", false),

  /** Alert has been seen and assigned; investigation underway. */
  ACKNOWLEDGED("Acknowledged", false),

  /** Alert has been actioned and closed. Terminal state. */
  RESOLVED("Resolved", true),

  /**
   * Alert is silenced (e.g. maintenance window, accepted risk). Not resolved. Can revert to
   * {@code OPEN}.
   */
  SUPPRESSED("Suppressed", false);

  // ── Fields ───────────────────────────────────────────────────────────────

  private final String displayName;
  private final boolean terminal;

  AlertStatus(String displayName, boolean terminal) {
    this.displayName = displayName;
    this.terminal = terminal;
  }

  // ── Accessors ────────────────────────────────────────────────────────────

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  public boolean isTerminal() {
    return terminal;
  }

  public boolean isActionable() {
    return this == OPEN || this == ACKNOWLEDGED;
  }

  // ── Transition Guard ─────────────────────────────────────────────────────

  public boolean canTransitionTo(AlertStatus target) {
    Set<AlertStatus> allowed =
        switch (this) {
          case OPEN -> Set.of(ACKNOWLEDGED, SUPPRESSED);
          case ACKNOWLEDGED -> Set.of(RESOLVED, SUPPRESSED);
          case SUPPRESSED -> Set.of(OPEN);
          case RESOLVED -> Set.of();
        };
    return allowed.contains(target);
  }

  // ── Deserialization ──────────────────────────────────────────────────────

  @JsonCreator
  public static AlertStatus fromDisplayName(String value) {
    if (value == null) throw new IllegalArgumentException("AlertStatus must not be null");
    return Arrays.stream(values())
        .filter(s -> s.name().equalsIgnoreCase(value) || s.displayName.equalsIgnoreCase(value))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unknown alert status: '"
                        + value
                        + "'. Valid: OPEN, ACKNOWLEDGED, RESOLVED, SUPPRESSED"));
  }
}
