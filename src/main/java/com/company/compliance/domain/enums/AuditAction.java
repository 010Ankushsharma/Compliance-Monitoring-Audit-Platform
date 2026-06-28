package com.company.compliance.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * All auditable actions recorded in the immutable {@code audit_logs} table.
 *
 * <p>Grouped by domain to keep the list navigable. The string value (returned by {@link
 * #getCode()}) is what gets stored in {@code audit_logs.action}.
 */
public enum AuditAction {

  // ── Authentication ───────────────────────────────────────────────────────
  LOGIN("LOGIN", AuditCategory.AUTH),
  LOGOUT("LOGOUT", AuditCategory.AUTH),
  LOGIN_FAILED("LOGIN_FAILED", AuditCategory.AUTH),
  TOKEN_REFRESHED("TOKEN_REFRESHED", AuditCategory.AUTH),
  TOKEN_REVOKED("TOKEN_REVOKED", AuditCategory.AUTH),
  MFA_ENABLED("MFA_ENABLED", AuditCategory.AUTH),
  MFA_DISABLED("MFA_DISABLED", AuditCategory.AUTH),
  PASSWORD_CHANGED("PASSWORD_CHANGED", AuditCategory.AUTH),
  ACCOUNT_LOCKED("ACCOUNT_LOCKED", AuditCategory.AUTH),
  ACCOUNT_UNLOCKED("ACCOUNT_UNLOCKED", AuditCategory.AUTH),

  // ── User Management ──────────────────────────────────────────────────────
  USER_CREATED("USER_CREATED", AuditCategory.USER_MANAGEMENT),
  USER_UPDATED("USER_UPDATED", AuditCategory.USER_MANAGEMENT),
  USER_DEACTIVATED("USER_DEACTIVATED", AuditCategory.USER_MANAGEMENT),
  USER_ROLE_CHANGED("USER_ROLE_CHANGED", AuditCategory.USER_MANAGEMENT),

  // ── Policy Management ────────────────────────────────────────────────────
  POLICY_CREATED("POLICY_CREATED", AuditCategory.POLICY),
  POLICY_UPDATED("POLICY_UPDATED", AuditCategory.POLICY),
  POLICY_ACTIVATED("POLICY_ACTIVATED", AuditCategory.POLICY),
  POLICY_DEACTIVATED("POLICY_DEACTIVATED", AuditCategory.POLICY),
  POLICY_ARCHIVED("POLICY_ARCHIVED", AuditCategory.POLICY),
  POLICY_RULE_ADDED("POLICY_RULE_ADDED", AuditCategory.POLICY),
  POLICY_RULE_UPDATED("POLICY_RULE_UPDATED", AuditCategory.POLICY),
  POLICY_RULE_REMOVED("POLICY_RULE_REMOVED", AuditCategory.POLICY),
  POLICY_EVALUATED("POLICY_EVALUATED", AuditCategory.POLICY),

  // ── Data Access ──────────────────────────────────────────────────────────
  DATA_ACCESS("DATA_ACCESS", AuditCategory.DATA),
  DATA_EXPORT("DATA_EXPORT", AuditCategory.DATA),
  DATA_DELETED("DATA_DELETED", AuditCategory.DATA),
  AUDIT_LOG_QUERIED("AUDIT_LOG_QUERIED", AuditCategory.DATA),
  AUDIT_LOG_EXPORTED("AUDIT_LOG_EXPORTED", AuditCategory.DATA),
  AUDIT_CHAIN_VERIFIED("AUDIT_CHAIN_VERIFIED", AuditCategory.DATA),

  // ── Violations ───────────────────────────────────────────────────────────
  VIOLATION_DETECTED("VIOLATION_DETECTED", AuditCategory.VIOLATION),
  VIOLATION_STATUS_CHANGED("VIOLATION_STATUS_CHANGED", AuditCategory.VIOLATION),
  VIOLATION_RESOLVED("VIOLATION_RESOLVED", AuditCategory.VIOLATION),
  VIOLATION_SUPPRESSED("VIOLATION_SUPPRESSED", AuditCategory.VIOLATION),
  EVIDENCE_ATTACHED("EVIDENCE_ATTACHED", AuditCategory.VIOLATION),

  // ── Alerts ───────────────────────────────────────────────────────────────
  ALERT_CREATED("ALERT_CREATED", AuditCategory.ALERT),
  ALERT_ACKNOWLEDGED("ALERT_ACKNOWLEDGED", AuditCategory.ALERT),
  ALERT_RESOLVED("ALERT_RESOLVED", AuditCategory.ALERT),
  ALERT_SUPPRESSED("ALERT_SUPPRESSED", AuditCategory.ALERT),
  NOTIFICATION_SENT("NOTIFICATION_SENT", AuditCategory.ALERT),
  NOTIFICATION_FAILED("NOTIFICATION_FAILED", AuditCategory.ALERT),

  // ── Reports ──────────────────────────────────────────────────────────────
  REPORT_REQUESTED("REPORT_REQUESTED", AuditCategory.REPORT),
  REPORT_GENERATED("REPORT_GENERATED", AuditCategory.REPORT),
  REPORT_DOWNLOADED("REPORT_DOWNLOADED", AuditCategory.REPORT),
  REPORT_FAILED("REPORT_FAILED", AuditCategory.REPORT),

  // ── System / Configuration ───────────────────────────────────────────────
  SYSTEM_CONFIG_CHANGED("SYSTEM_CONFIG_CHANGED", AuditCategory.SYSTEM),
  NOTIFICATION_CHANNEL_CREATED("NOTIFICATION_CHANNEL_CREATED", AuditCategory.SYSTEM),
  NOTIFICATION_CHANNEL_UPDATED("NOTIFICATION_CHANNEL_UPDATED", AuditCategory.SYSTEM),
  NOTIFICATION_CHANNEL_DELETED("NOTIFICATION_CHANNEL_DELETED", AuditCategory.SYSTEM),
  RISK_SCORE_COMPUTED("RISK_SCORE_COMPUTED", AuditCategory.SYSTEM);

  // ── Inner Enum: Category ──────────────────────────────────────────────────

  public enum AuditCategory {
    AUTH,
    USER_MANAGEMENT,
    POLICY,
    DATA,
    VIOLATION,
    ALERT,
    REPORT,
    SYSTEM
  }

  // ── Fields ────────────────────────────────────────────────────────────────

  private final String code;
  private final AuditCategory category;

  AuditAction(String code, AuditCategory category) {
    this.code = code;
    this.category = category;
  }

  // ── Accessors ─────────────────────────────────────────────────────────────

  /** The string stored in {@code audit_logs.action}. */
  @JsonValue
  public String getCode() {
    return code;
  }

  public AuditCategory getCategory() {
    return category;
  }

  // ── Utility ───────────────────────────────────────────────────────────────

  /** Returns {@code true} if this action represents a security-sensitive event. */
  public boolean isSecuritySensitive() {
    return this == LOGIN_FAILED
        || this == ACCOUNT_LOCKED
        || this == MFA_DISABLED
        || this == USER_ROLE_CHANGED
        || this == SYSTEM_CONFIG_CHANGED
        || this == AUDIT_CHAIN_VERIFIED;
  }

  /** Looks up an {@code AuditAction} by its stored code string. */
  public static AuditAction fromCode(String code) {
    for (AuditAction action : values()) {
      if (action.code.equalsIgnoreCase(code)) return action;
    }
    throw new IllegalArgumentException("Unknown audit action code: '" + code + "'");
  }
}
