-- =============================================================
--  V3__add_risk_scoring.sql
--  Compliance Monitoring & Audit Platform — Risk Scoring Layer
--
--  Additions:
--    1. risk_scores          — per-policy rolling risk score ledger
--    2. risk_score_history   — time-series snapshots for trend charts
--    3. policy_evaluations   — record of each scheduled policy run
--    4. evidence_attachments — files/screenshots attached to violations
--    5. compliance_score_snapshot (materialized view) — fast dashboard reads
--    6. Columns added to existing tables (ALTER TABLE)
--    7. Supporting indexes
-- =============================================================

-- =============================================================
-- 1. RISK SCORES
--    One current score row per organization+policy.
--    Updated in-place on each evaluation run.
-- =============================================================
CREATE TABLE risk_scores (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    policy_id        UUID          NOT NULL REFERENCES policies (id)        ON DELETE CASCADE,
    framework        VARCHAR(50)   NOT NULL,

    -- Score components (0.0 – 100.0; 100 = fully compliant)
    compliance_score         NUMERIC(5,2)  NOT NULL DEFAULT 0.00
                                 CHECK (compliance_score BETWEEN 0 AND 100),
    violation_penalty        NUMERIC(5,2)  NOT NULL DEFAULT 0.00
                                 CHECK (violation_penalty BETWEEN 0 AND 100),
    open_violations_count    INTEGER       NOT NULL DEFAULT 0,
    critical_violations      INTEGER       NOT NULL DEFAULT 0,
    high_violations          INTEGER       NOT NULL DEFAULT 0,
    medium_violations        INTEGER       NOT NULL DEFAULT 0,
    low_violations           INTEGER       NOT NULL DEFAULT 0,

    -- Trend
    previous_score           NUMERIC(5,2),
    score_delta              NUMERIC(6,2)  GENERATED ALWAYS AS
                                 (compliance_score - COALESCE(previous_score, compliance_score)) STORED,
    trend                    VARCHAR(10)   CHECK (trend IN ('IMPROVING','DECLINING','STABLE')),

    last_evaluated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    next_evaluation_at       TIMESTAMPTZ,
    created_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_risk_scores_org_policy UNIQUE (organization_id, policy_id)
);

CREATE INDEX idx_risk_scores_org           ON risk_scores (organization_id);
CREATE INDEX idx_risk_scores_framework     ON risk_scores (framework);
CREATE INDEX idx_risk_scores_score         ON risk_scores (compliance_score);
CREATE INDEX idx_risk_scores_next_eval     ON risk_scores (next_evaluation_at)
                                           WHERE next_evaluation_at IS NOT NULL;

CREATE TRIGGER trg_risk_scores_updated_at
    BEFORE UPDATE ON risk_scores
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- =============================================================
-- 2. RISK SCORE HISTORY
--    Append-only snapshots — one row per evaluation run.
--    Powers trend charts and executive dashboards.
-- =============================================================
CREATE TABLE risk_score_history (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    risk_score_id    UUID          NOT NULL REFERENCES risk_scores (id) ON DELETE CASCADE,
    organization_id  UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    policy_id        UUID          NOT NULL REFERENCES policies (id)        ON DELETE CASCADE,
    framework        VARCHAR(50)   NOT NULL,
    compliance_score NUMERIC(5,2)  NOT NULL CHECK (compliance_score BETWEEN 0 AND 100),
    open_violations  INTEGER       NOT NULL DEFAULT 0,
    critical_count   INTEGER       NOT NULL DEFAULT 0,
    snapshot_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

-- Time-series queries: org + policy + time window
CREATE INDEX idx_rsh_org_policy_ts   ON risk_score_history (organization_id, policy_id, snapshot_at DESC);
CREATE INDEX idx_rsh_framework_ts    ON risk_score_history (framework, snapshot_at DESC);

-- =============================================================
-- 3. POLICY EVALUATIONS
--    Audit trail of every scheduled or manual policy run.
-- =============================================================
CREATE TABLE policy_evaluations (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    policy_id        UUID          NOT NULL REFERENCES policies (id)        ON DELETE CASCADE,
    triggered_by     UUID          REFERENCES users (id) ON DELETE SET NULL,  -- NULL = scheduler
    trigger_type     VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED'
                         CHECK (trigger_type IN ('SCHEDULED','MANUAL','EVENT_DRIVEN')),
    status           VARCHAR(20)   NOT NULL DEFAULT 'RUNNING'
                         CHECK (status IN ('RUNNING','COMPLETED','FAILED','CANCELLED')),
    rules_evaluated  INTEGER       NOT NULL DEFAULT 0,
    violations_found INTEGER       NOT NULL DEFAULT 0,
    violations_new   INTEGER       NOT NULL DEFAULT 0,
    started_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    completed_at     TIMESTAMPTZ,
    duration_ms      INTEGER       GENERATED ALWAYS AS (
                         EXTRACT(EPOCH FROM (completed_at - started_at)) * 1000
                     )::INTEGER STORED,
    error_message    TEXT,
    metadata         JSONB
);

CREATE INDEX idx_policy_eval_org_policy  ON policy_evaluations (organization_id, policy_id);
CREATE INDEX idx_policy_eval_started     ON policy_evaluations (started_at DESC);
CREATE INDEX idx_policy_eval_status      ON policy_evaluations (status)
                                         WHERE status IN ('RUNNING','FAILED');

-- =============================================================
-- 4. EVIDENCE ATTACHMENTS
--    Files or screenshots attached to violation records.
-- =============================================================
CREATE TABLE evidence_attachments (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    violation_id     UUID          NOT NULL REFERENCES compliance_violations (id) ON DELETE CASCADE,
    uploaded_by      UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    file_name        VARCHAR(500)  NOT NULL,
    file_type        VARCHAR(100)  NOT NULL,   -- MIME type
    file_size_bytes  BIGINT        NOT NULL,
    storage_key      VARCHAR(1000) NOT NULL,   -- local path or S3 object key
    description      TEXT,
    uploaded_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_file_size CHECK (file_size_bytes > 0 AND file_size_bytes <= 52428800) -- max 50 MB
);

CREATE INDEX idx_evidence_violation   ON evidence_attachments (violation_id);
CREATE INDEX idx_evidence_uploaded_by ON evidence_attachments (uploaded_by);

-- =============================================================
-- 5. ALTER EXISTING TABLES — additive columns only
-- =============================================================

-- Add risk_score column to compliance_violations (back-filled to NULL)
ALTER TABLE compliance_violations
    ADD COLUMN IF NOT EXISTS policy_evaluation_id UUID
        REFERENCES policy_evaluations (id) ON DELETE SET NULL;

CREATE INDEX idx_violations_eval  ON compliance_violations (policy_evaluation_id)
                                  WHERE policy_evaluation_id IS NOT NULL;

-- Add overall_risk_score to organizations for single-number dashboard tile
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS overall_risk_score  NUMERIC(5,2)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS risk_last_updated   TIMESTAMPTZ   DEFAULT NULL;

-- Add evaluation schedule to policies (cron expression)
ALTER TABLE policies
    ADD COLUMN IF NOT EXISTS evaluation_schedule VARCHAR(100) DEFAULT '0 0 * * *',  -- daily midnight
    ADD COLUMN IF NOT EXISTS last_evaluated_at   TIMESTAMPTZ  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS next_evaluation_at  TIMESTAMPTZ  DEFAULT NULL;

CREATE INDEX idx_policies_next_eval  ON policies (next_evaluation_at)
                                     WHERE next_evaluation_at IS NOT NULL AND status = 'ACTIVE';

-- =============================================================
-- 6. MATERIALIZED VIEW — compliance_score_snapshot
--    Pre-aggregates per-org, per-framework scores for fast
--    dashboard reads. Refreshed by the scheduler service.
-- =============================================================
CREATE MATERIALIZED VIEW compliance_score_snapshot AS
SELECT
    o.id                                                        AS organization_id,
    o.name                                                      AS organization_name,
    rs.framework,
    COUNT(rs.id)                                                AS total_policies,
    ROUND(AVG(rs.compliance_score), 2)                         AS avg_compliance_score,
    SUM(rs.open_violations_count)                               AS total_open_violations,
    SUM(rs.critical_violations)                                 AS total_critical,
    SUM(rs.high_violations)                                     AS total_high,
    SUM(rs.medium_violations)                                   AS total_medium,
    SUM(rs.low_violations)                                      AS total_low,
    MAX(rs.last_evaluated_at)                                   AS last_evaluated_at,
    NOW()                                                       AS snapshot_at
FROM risk_scores rs
JOIN organizations o ON o.id = rs.organization_id
WHERE o.active = TRUE
  AND o.deleted_at IS NULL
GROUP BY o.id, o.name, rs.framework
WITH DATA;

-- Index the materialized view
CREATE UNIQUE INDEX idx_css_org_framework
    ON compliance_score_snapshot (organization_id, framework);

CREATE INDEX idx_css_score
    ON compliance_score_snapshot (avg_compliance_score);

-- =============================================================
-- 7. SEED: initial risk_score rows for seeded policies
--    (score starts at 0; first evaluation will set real values)
-- =============================================================
INSERT INTO risk_scores (
    id, organization_id, policy_id, framework,
    compliance_score, next_evaluation_at
)
SELECT
    gen_random_uuid(),
    'a0000000-0000-0000-0000-000000000001',
    p.id,
    p.framework,
    0.00,
    NOW() + INTERVAL '1 hour'
FROM policies p
WHERE p.organization_id = 'a0000000-0000-0000-0000-000000000001'
ON CONFLICT (organization_id, policy_id) DO NOTHING;

-- =============================================================
-- COMMENTS
-- =============================================================
COMMENT ON TABLE risk_scores             IS 'Current compliance/risk score per org+policy; updated on each evaluation';
COMMENT ON TABLE risk_score_history      IS 'Append-only score snapshots powering trend charts';
COMMENT ON TABLE policy_evaluations      IS 'Audit trail of every policy evaluation run (scheduled or manual)';
COMMENT ON TABLE evidence_attachments    IS 'Files and screenshots attached to violation records as evidence';
COMMENT ON MATERIALIZED VIEW compliance_score_snapshot IS
    'Pre-aggregated compliance scores per org+framework. Refresh with: REFRESH MATERIALIZED VIEW CONCURRENTLY compliance_score_snapshot';
