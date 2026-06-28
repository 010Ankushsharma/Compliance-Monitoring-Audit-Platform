-- =============================================================
--  V1__init_schema.sql
--  Compliance Monitoring & Audit Platform — Initial Schema
--
--  Tables created (in dependency order):
--    1.  organizations
--    2.  users
--    3.  refresh_tokens
--    4.  policies
--    5.  policy_rules
--    6.  audit_logs
--    7.  compliance_violations
--    8.  alerts
--    9.  reports
--   10.  report_frameworks  (join table)
--   11.  notification_channels
--
--  Every table has:
--    • UUID primary key (gen_random_uuid())
--    • created_at / updated_at with auto-update trigger
--    • Soft-delete via deleted_at where applicable
--    • Appropriate indexes for common query patterns
-- =============================================================

-- Enable UUID generation (PostgreSQL 13+: gen_random_uuid() built-in)
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================
-- UTILITY: auto-update updated_at on every row change
-- =============================================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================================
-- 1. ORGANIZATIONS
-- =============================================================
CREATE TABLE organizations (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(255)  NOT NULL,
    industry              VARCHAR(100),
    country               VARCHAR(100),
    -- comma-separated list stored as text array
    regulatory_frameworks TEXT[]        NOT NULL DEFAULT '{}',
    contact_email         VARCHAR(255),
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at            TIMESTAMPTZ,

    CONSTRAINT uq_organization_name UNIQUE (name)
);

CREATE INDEX idx_organizations_active      ON organizations (active)      WHERE deleted_at IS NULL;
CREATE INDEX idx_organizations_industry    ON organizations (industry)    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_organizations_updated_at
    BEFORE UPDATE ON organizations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 2. USERS
-- =============================================================
CREATE TABLE users (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID          NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    email           VARCHAR(255)  NOT NULL,
    full_name       VARCHAR(255)  NOT NULL,
    password_hash   VARCHAR(512)  NOT NULL,
    role            VARCHAR(50)   NOT NULL
                        CHECK (role IN ('SUPER_ADMIN','COMPLIANCE_OFFICER','AUDITOR','ANALYST','API_CLIENT')),
    mfa_enabled     BOOLEAN       NOT NULL DEFAULT FALSE,
    mfa_secret      VARCHAR(255),                          -- TOTP secret (encrypted at app layer)
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMPTZ,
    failed_logins   SMALLINT      NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,                           -- account lockout
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,

    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_organization    ON users (organization_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role            ON users (role)            WHERE deleted_at IS NULL;
CREATE INDEX idx_users_active          ON users (active)          WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email_lower     ON users (LOWER(email));

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 3. REFRESH TOKENS
-- =============================================================
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(512) NOT NULL,                     -- SHA-256 hash of the raw token
    issued_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMPTZ,
    ip_address  VARCHAR(45),                               -- IPv4 or IPv6
    user_agent  TEXT,

    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user       ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires    ON refresh_tokens (expires_at) WHERE revoked = FALSE;

-- =============================================================
-- 4. POLICIES
-- =============================================================
CREATE TABLE policies (
    id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID          NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    name                VARCHAR(255)  NOT NULL,
    description         TEXT,
    framework           VARCHAR(50)   NOT NULL
                            CHECK (framework IN ('ISO_27001','SOC2','GDPR','HIPAA','PCI_DSS','NIST','CUSTOM')),
    severity            VARCHAR(20)   NOT NULL
                            CHECK (severity IN ('CRITICAL','HIGH','MEDIUM','LOW','INFO')),
    status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT'
                            CHECK (status IN ('DRAFT','ACTIVE','INACTIVE','ARCHIVED')),
    version             INTEGER       NOT NULL DEFAULT 1,
    effective_date      DATE,
    expiry_date         DATE,
    owner_id            UUID          REFERENCES users (id) ON DELETE SET NULL,
    tags                TEXT[]        NOT NULL DEFAULT '{}',
    created_by          UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,

    CONSTRAINT uq_policy_name_org UNIQUE (organization_id, name)
);

CREATE INDEX idx_policies_org         ON policies (organization_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_policies_framework   ON policies (framework)       WHERE deleted_at IS NULL;
CREATE INDEX idx_policies_status      ON policies (status)          WHERE deleted_at IS NULL;
CREATE INDEX idx_policies_severity    ON policies (severity)        WHERE deleted_at IS NULL;
CREATE INDEX idx_policies_owner       ON policies (owner_id)        WHERE deleted_at IS NULL;
CREATE INDEX idx_policies_tags        ON policies USING GIN (tags);

CREATE TRIGGER trg_policies_updated_at
    BEFORE UPDATE ON policies
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 5. POLICY RULES
-- =============================================================
CREATE TABLE policy_rules (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id         UUID         NOT NULL REFERENCES policies (id) ON DELETE CASCADE,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    rule_type         VARCHAR(30)  NOT NULL
                          CHECK (rule_type IN ('THRESHOLD','PATTERN','PRESENCE','FREQUENCY','CUSTOM')),
    field             VARCHAR(255) NOT NULL,
    operator          VARCHAR(20)  NOT NULL
                          CHECK (operator IN ('EQUALS','NOT_EQUALS','GREATER_THAN','LESS_THAN',
                                              'CONTAINS','NOT_CONTAINS','MATCHES_REGEX','IN','NOT_IN')),
    value             TEXT         NOT NULL,
    grace_period_days INTEGER      NOT NULL DEFAULT 0,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    evaluation_order  SMALLINT     NOT NULL DEFAULT 0,     -- lower = evaluated first
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_policy_rules_policy  ON policy_rules (policy_id);
CREATE INDEX idx_policy_rules_active  ON policy_rules (policy_id, active);

CREATE TRIGGER trg_policy_rules_updated_at
    BEFORE UPDATE ON policy_rules
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 6. AUDIT LOGS
--    Immutable — no UPDATE/DELETE permitted (enforced by trigger)
--    SHA-256 hash chain for tamper evidence
-- =============================================================
CREATE TABLE audit_logs (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID          NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    timestamp       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    user_id         UUID          REFERENCES users (id) ON DELETE SET NULL,
    user_email      VARCHAR(255),                          -- denormalised: preserved if user deleted
    action          VARCHAR(100)  NOT NULL,                -- e.g. DATA_ACCESS, LOGIN, POLICY_CHANGE
    resource_type   VARCHAR(100),                          -- e.g. CUSTOMER_RECORD, POLICY, REPORT
    resource_id     VARCHAR(255),
    resource_name   VARCHAR(500),
    http_method     VARCHAR(10),
    endpoint        VARCHAR(500),
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    request_id      VARCHAR(100),                          -- correlation ID from X-Request-ID header
    outcome         VARCHAR(20)   NOT NULL DEFAULT 'SUCCESS'
                        CHECK (outcome IN ('SUCCESS','FAILURE','ERROR')),
    status_code     SMALLINT,
    duration_ms     INTEGER,
    details         JSONB,                                 -- arbitrary extra context
    -- Hash chain
    hash            VARCHAR(128)  NOT NULL,                -- SHA-256 of this row's canonical fields
    previous_hash   VARCHAR(128),                          -- SHA-256 of the previous row
    sequence_number BIGSERIAL     NOT NULL UNIQUE          -- monotonic ordering for chain walks
);

-- Partial indexes tuned for most common queries
CREATE INDEX idx_audit_logs_org_ts         ON audit_logs (organization_id, timestamp DESC);
CREATE INDEX idx_audit_logs_user           ON audit_logs (user_id, timestamp DESC);
CREATE INDEX idx_audit_logs_action         ON audit_logs (action, timestamp DESC);
CREATE INDEX idx_audit_logs_resource       ON audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_logs_outcome        ON audit_logs (outcome)        WHERE outcome != 'SUCCESS';
CREATE INDEX idx_audit_logs_request_id     ON audit_logs (request_id)     WHERE request_id IS NOT NULL;
CREATE INDEX idx_audit_logs_details        ON audit_logs USING GIN (details);

-- Immutability enforcement
CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs rows are immutable — UPDATE and DELETE are forbidden';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_logs_immutable_update
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_mutation();

CREATE TRIGGER trg_audit_logs_immutable_delete
    BEFORE DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_mutation();

-- =============================================================
-- 7. COMPLIANCE VIOLATIONS
-- =============================================================
CREATE TABLE compliance_violations (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID          NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    policy_id       UUID          NOT NULL REFERENCES policies (id) ON DELETE RESTRICT,
    policy_rule_id  UUID          REFERENCES policy_rules (id) ON DELETE SET NULL,
    audit_log_id    UUID          REFERENCES audit_logs (id) ON DELETE SET NULL,
    user_id         UUID          REFERENCES users (id) ON DELETE SET NULL,
    severity        VARCHAR(20)   NOT NULL
                        CHECK (severity IN ('CRITICAL','HIGH','MEDIUM','LOW','INFO')),
    status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN','IN_REVIEW','RESOLVED','FALSE_POSITIVE','SUPPRESSED')),
    title           VARCHAR(500)  NOT NULL,
    description     TEXT,
    evidence        JSONB,                                 -- captured context at time of detection
    detected_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    acknowledged_by UUID          REFERENCES users (id) ON DELETE SET NULL,
    acknowledged_at TIMESTAMPTZ,
    resolved_by     UUID          REFERENCES users (id) ON DELETE SET NULL,
    resolved_at     TIMESTAMPTZ,
    resolution_note TEXT,
    risk_score      NUMERIC(5,2)  CHECK (risk_score BETWEEN 0 AND 100),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_violations_org_status   ON compliance_violations (organization_id, status);
CREATE INDEX idx_violations_policy       ON compliance_violations (policy_id);
CREATE INDEX idx_violations_severity     ON compliance_violations (severity, detected_at DESC);
CREATE INDEX idx_violations_user         ON compliance_violations (user_id)  WHERE user_id IS NOT NULL;
CREATE INDEX idx_violations_detected     ON compliance_violations (detected_at DESC);
CREATE INDEX idx_violations_open         ON compliance_violations (organization_id, detected_at DESC)
                                         WHERE status = 'OPEN';
CREATE INDEX idx_violations_evidence     ON compliance_violations USING GIN (evidence);

CREATE TRIGGER trg_violations_updated_at
    BEFORE UPDATE ON compliance_violations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 8. ALERTS
-- =============================================================
CREATE TABLE alerts (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID         NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    violation_id     UUID         REFERENCES compliance_violations (id) ON DELETE SET NULL,
    severity         VARCHAR(20)  NOT NULL
                         CHECK (severity IN ('CRITICAL','HIGH','MEDIUM','LOW','INFO')),
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                         CHECK (status IN ('OPEN','ACKNOWLEDGED','RESOLVED','SUPPRESSED')),
    title            VARCHAR(500) NOT NULL,
    message          TEXT         NOT NULL,
    source           VARCHAR(100),                         -- which service generated the alert
    -- Deduplication
    dedup_key        VARCHAR(255),                         -- deterministic key to suppress duplicates
    suppressed_until TIMESTAMPTZ,
    -- Escalation
    escalation_level SMALLINT     NOT NULL DEFAULT 0,
    escalated_at     TIMESTAMPTZ,
    -- Resolution
    acknowledged_by  UUID         REFERENCES users (id) ON DELETE SET NULL,
    acknowledged_at  TIMESTAMPTZ,
    resolved_by      UUID         REFERENCES users (id) ON DELETE SET NULL,
    resolved_at      TIMESTAMPTZ,
    -- Notification tracking
    notification_sent         BOOLEAN      NOT NULL DEFAULT FALSE,
    notification_sent_at      TIMESTAMPTZ,
    notification_channels     TEXT[]       NOT NULL DEFAULT '{}',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_org_status      ON alerts (organization_id, status);
CREATE INDEX idx_alerts_severity        ON alerts (severity, created_at DESC);
CREATE INDEX idx_alerts_violation       ON alerts (violation_id) WHERE violation_id IS NOT NULL;
CREATE INDEX idx_alerts_open            ON alerts (organization_id, created_at DESC) WHERE status = 'OPEN';
CREATE INDEX idx_alerts_dedup           ON alerts (dedup_key)    WHERE dedup_key IS NOT NULL;
CREATE INDEX idx_alerts_notification    ON alerts (notification_sent, created_at)
                                        WHERE notification_sent = FALSE AND status = 'OPEN';

CREATE TRIGGER trg_alerts_updated_at
    BEFORE UPDATE ON alerts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 9. REPORTS
-- =============================================================
CREATE TABLE reports (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID          NOT NULL REFERENCES organizations (id) ON DELETE RESTRICT,
    template_id     VARCHAR(100)  NOT NULL,                -- e.g. soc2-type2, iso27001-gap
    title           VARCHAR(500)  NOT NULL,
    description     TEXT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','GENERATING','COMPLETED','FAILED')),
    format          VARCHAR(10)   NOT NULL DEFAULT 'PDF'
                        CHECK (format IN ('PDF','EXCEL','CSV','JSON')),
    period_start    DATE          NOT NULL,
    period_end      DATE          NOT NULL,
    include_evidence BOOLEAN      NOT NULL DEFAULT TRUE,
    -- File storage
    file_path       VARCHAR(1000),                         -- local path or S3 key
    file_size_bytes BIGINT,
    -- Generation metadata
    generated_by    UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    error_message   TEXT,
    -- Summary stats embedded in the report row for quick dashboard reads
    summary         JSONB,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_report_period CHECK (period_start <= period_end)
);

CREATE INDEX idx_reports_org_status    ON reports (organization_id, status);
CREATE INDEX idx_reports_generated_by  ON reports (generated_by);
CREATE INDEX idx_reports_created       ON reports (created_at DESC);
CREATE INDEX idx_reports_pending       ON reports (created_at)
                                       WHERE status IN ('PENDING','GENERATING');

CREATE TRIGGER trg_reports_updated_at
    BEFORE UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 10. REPORT_FRAMEWORKS  (many-to-many: report ↔ framework)
-- =============================================================
CREATE TABLE report_frameworks (
    report_id    UUID        NOT NULL REFERENCES reports (id) ON DELETE CASCADE,
    framework    VARCHAR(50) NOT NULL
                     CHECK (framework IN ('ISO_27001','SOC2','GDPR','HIPAA','PCI_DSS','NIST','CUSTOM')),

    PRIMARY KEY (report_id, framework)
);

CREATE INDEX idx_report_frameworks_framework ON report_frameworks (framework);

-- =============================================================
-- 11. NOTIFICATION_CHANNELS
--     Per-organization channel configuration
-- =============================================================
CREATE TABLE notification_channels (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    channel_type    VARCHAR(30)   NOT NULL
                        CHECK (channel_type IN ('EMAIL','SLACK','WEBHOOK','SMS','PAGERDUTY')),
    name            VARCHAR(255)  NOT NULL,
    -- Encrypted configuration stored as JSONB
    -- e.g. {"webhook_url":"...","token":"..."} — encrypted at app layer before insert
    config          JSONB         NOT NULL,
    min_severity    VARCHAR(20)   NOT NULL DEFAULT 'HIGH'
                        CHECK (min_severity IN ('CRITICAL','HIGH','MEDIUM','LOW','INFO')),
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_channels_org    ON notification_channels (organization_id) WHERE active = TRUE;
CREATE INDEX idx_notification_channels_type   ON notification_channels (channel_type)    WHERE active = TRUE;

CREATE TRIGGER trg_notification_channels_updated_at
    BEFORE UPDATE ON notification_channels
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- COMMENTS (for DBA / tooling visibility)
-- =============================================================
COMMENT ON TABLE organizations           IS 'Tenant root — all data scoped to an organization';
COMMENT ON TABLE users                   IS 'Platform users with RBAC roles';
COMMENT ON TABLE refresh_tokens          IS 'JWT refresh token store with revocation support';
COMMENT ON TABLE policies                IS 'Compliance policies mapped to regulatory frameworks';
COMMENT ON TABLE policy_rules            IS 'Individual rules within a policy; evaluated in order';
COMMENT ON TABLE audit_logs              IS 'Immutable, hash-chained activity log — no UPDATE/DELETE';
COMMENT ON TABLE compliance_violations   IS 'Policy violations detected by the violation engine';
COMMENT ON TABLE alerts                  IS 'Actionable notifications derived from violations';
COMMENT ON TABLE reports                 IS 'Generated compliance evidence reports';
COMMENT ON TABLE report_frameworks       IS 'Regulatory frameworks covered by a report';
COMMENT ON TABLE notification_channels   IS 'Per-org notification channel config (email/Slack/webhook)';

COMMENT ON COLUMN audit_logs.hash          IS 'SHA-256 of canonical row fields; chained for tamper detection';
COMMENT ON COLUMN audit_logs.previous_hash IS 'SHA-256 hash of the preceding audit_log row';
COMMENT ON COLUMN users.mfa_secret         IS 'TOTP secret encrypted at the application layer before storage';
COMMENT ON COLUMN notification_channels.config IS 'Channel credentials encrypted at application layer';
