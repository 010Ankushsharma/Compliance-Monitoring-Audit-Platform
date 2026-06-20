-- =============================================================
--  V2__seed_frameworks.sql
--  Compliance Monitoring & Audit Platform — Reference Data Seed
--
--  Seeds:
--    1. Default organization
--    2. Super-admin user           (password must be changed on first login)
--    3. Pre-built policies for each supported regulatory framework
--    4. Sample policy rules per framework
--    5. Default notification channel (email)
--
--  All UUIDs are fixed so this script is idempotent when
--  re-run in non-prod environments via INSERT ... ON CONFLICT DO NOTHING.
-- =============================================================

-- =============================================================
-- 1. DEFAULT ORGANIZATION
-- =============================================================
INSERT INTO organizations (
    id,
    name,
    industry,
    country,
    regulatory_frameworks,
    contact_email,
    active
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Default Organization',
    'Technology',
    'US',
    ARRAY['ISO_27001','SOC2','GDPR','HIPAA'],
    'admin@company.com',
    TRUE
) ON CONFLICT (name) DO NOTHING;

-- =============================================================
-- 2. SUPER-ADMIN USER
--    Password: Admin@1234  (bcrypt, cost=12)
--    MUST be rotated immediately after first login.
-- =============================================================
INSERT INTO users (
    id,
    organization_id,
    email,
    full_name,
    password_hash,
    role,
    mfa_enabled,
    active
) VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'admin@company.com',
    'Platform Super Admin',
    -- bcrypt(Admin@1234, cost=12) — change immediately after first login
    '$2a$12$xkXBiXNOcULXlQ/rMTVUOeXEMJf5.E9eANq9TBSyeXIMqcVZNWAEu',
    'SUPER_ADMIN',
    FALSE,
    TRUE
) ON CONFLICT (email) DO NOTHING;

-- =============================================================
-- 3. FRAMEWORK POLICIES
--    One canonical policy per supported framework.
--    All seeded as ACTIVE so they apply immediately.
-- =============================================================

-- ── ISO 27001 ────────────────────────────────────────────────
INSERT INTO policies (
    id, organization_id, name, description, framework,
    severity, status, version, effective_date, created_by
) VALUES (
    'c0000000-0000-0000-0001-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'ISO 27001 — Information Security Controls',
    'Core information security controls aligned with ISO/IEC 27001:2022. '
    'Covers access management, cryptography, asset management, and incident response.',
    'ISO_27001', 'HIGH', 'ACTIVE', 1, CURRENT_DATE,
    'b0000000-0000-0000-0000-000000000001'
) ON CONFLICT (organization_id, name) DO NOTHING;

-- ── SOC 2 ────────────────────────────────────────────────────
INSERT INTO policies (
    id, organization_id, name, description, framework,
    severity, status, version, effective_date, created_by
) VALUES (
    'c0000000-0000-0000-0001-000000000002',
    'a0000000-0000-0000-0000-000000000001',
    'SOC 2 Type II — Trust Services Criteria',
    'Controls for the SOC 2 Trust Services Criteria: Security (CC), Availability (A), '
    'Confidentiality (C), Processing Integrity (PI), and Privacy (P).',
    'SOC2', 'HIGH', 'ACTIVE', 1, CURRENT_DATE,
    'b0000000-0000-0000-0000-000000000001'
) ON CONFLICT (organization_id, name) DO NOTHING;

-- ── GDPR ─────────────────────────────────────────────────────
INSERT INTO policies (
    id, organization_id, name, description, framework,
    severity, status, version, effective_date, created_by
) VALUES (
    'c0000000-0000-0000-0001-000000000003',
    'a0000000-0000-0000-0000-000000000001',
    'GDPR — General Data Protection Regulation',
    'Controls for EU GDPR compliance covering lawful basis, data subject rights, '
    'privacy by design, DPO requirements, and breach notification obligations.',
    'GDPR', 'CRITICAL', 'ACTIVE', 1, CURRENT_DATE,
    'b0000000-0000-0000-0000-000000000001'
) ON CONFLICT (organization_id, name) DO NOTHING;

-- ── HIPAA ────────────────────────────────────────────────────
INSERT INTO policies (
    id, organization_id, name, description, framework,
    severity, status, version, effective_date, created_by
) VALUES (
    'c0000000-0000-0000-0001-000000000004',
    'a0000000-0000-0000-0000-000000000001',
    'HIPAA — Health Information Privacy & Security',
    'Administrative, physical, and technical safeguards for Protected Health Information (PHI) '
    'under the HIPAA Privacy Rule and Security Rule.',
    'HIPAA', 'CRITICAL', 'ACTIVE', 1, CURRENT_DATE,
    'b0000000-0000-0000-0000-000000000001'
) ON CONFLICT (organization_id, name) DO NOTHING;

-- ── PCI DSS ──────────────────────────────────────────────────
INSERT INTO policies (
    id, organization_id, name, description, framework,
    severity, status, version, effective_date, created_by
) VALUES (
    'c0000000-0000-0000-0001-000000000005',
    'a0000000-0000-0000-0000-000000000001',
    'PCI DSS v4.0 — Payment Card Industry Standards',
    'Controls for securing cardholder data environments under PCI DSS v4.0. '
    'Covers network security, access control, vulnerability management, and monitoring.',
    'PCI_DSS', 'CRITICAL', 'ACTIVE', 1, CURRENT_DATE,
    'b0000000-0000-0000-0000-000000000001'
) ON CONFLICT (organization_id, name) DO NOTHING;

-- ── NIST CSF ─────────────────────────────────────────────────
INSERT INTO policies (
    id, organization_id, name, description, framework,
    severity, status, version, effective_date, created_by
) VALUES (
    'c0000000-0000-0000-0001-000000000006',
    'a0000000-0000-0000-0000-000000000001',
    'NIST CSF 2.0 — Cybersecurity Framework',
    'Controls aligned with NIST Cybersecurity Framework 2.0 functions: Govern, Identify, '
    'Protect, Detect, Respond, and Recover.',
    'NIST', 'HIGH', 'ACTIVE', 1, CURRENT_DATE,
    'b0000000-0000-0000-0000-000000000001'
) ON CONFLICT (organization_id, name) DO NOTHING;

-- =============================================================
-- 4. POLICY RULES — representative rules per framework
-- =============================================================

-- ── ISO 27001 Rules ──────────────────────────────────────────
INSERT INTO policy_rules (id, policy_id, name, rule_type, field, operator, value, grace_period_days, evaluation_order)
VALUES
    ('d0000001-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0001-000000000001',
     'MFA Enforcement',
     'PRESENCE', 'user.mfa_enabled', 'EQUALS', 'true', 7, 1),

    ('d0000001-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0001-000000000001',
     'Password Minimum Length',
     'THRESHOLD', 'password.length', 'GREATER_THAN', '12', 0, 2),

    ('d0000001-0000-0000-0000-000000000003',
     'c0000000-0000-0000-0001-000000000001',
     'Session Idle Timeout',
     'THRESHOLD', 'session.idle_timeout_minutes', 'LESS_THAN', '30', 0, 3),

    ('d0000001-0000-0000-0000-000000000004',
     'c0000000-0000-0000-0001-000000000001',
     'Privileged Access Review Frequency',
     'FREQUENCY', 'access_review.days_since_last', 'LESS_THAN', '90', 0, 4),

    ('d0000001-0000-0000-0000-000000000005',
     'c0000000-0000-0000-0001-000000000001',
     'Audit Log Retention',
     'THRESHOLD', 'audit.retention_days', 'GREATER_THAN', '364', 0, 5)
ON CONFLICT DO NOTHING;

-- ── SOC 2 Rules ──────────────────────────────────────────────
INSERT INTO policy_rules (id, policy_id, name, rule_type, field, operator, value, grace_period_days, evaluation_order)
VALUES
    ('d0000002-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0001-000000000002',
     'Encryption at Rest',
     'PRESENCE', 'storage.encryption_enabled', 'EQUALS', 'true', 0, 1),

    ('d0000002-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0001-000000000002',
     'Encryption in Transit (TLS 1.2+)',
     'PRESENCE', 'transport.tls_version', 'GREATER_THAN', '1.1', 0, 2),

    ('d0000002-0000-0000-0000-000000000003',
     'c0000000-0000-0000-0001-000000000002',
     'Vulnerability Scan Frequency',
     'FREQUENCY', 'vuln_scan.days_since_last', 'LESS_THAN', '30', 0, 3),

    ('d0000002-0000-0000-0000-000000000004',
     'c0000000-0000-0000-0001-000000000002',
     'Uptime SLA',
     'THRESHOLD', 'availability.uptime_percent', 'GREATER_THAN', '99.5', 0, 4),

    ('d0000002-0000-0000-0000-000000000005',
     'c0000000-0000-0000-0001-000000000002',
     'Incident Response Time (Critical)',
     'THRESHOLD', 'incident.response_minutes', 'LESS_THAN', '60', 0, 5)
ON CONFLICT DO NOTHING;

-- ── GDPR Rules ───────────────────────────────────────────────
INSERT INTO policy_rules (id, policy_id, name, rule_type, field, operator, value, grace_period_days, evaluation_order)
VALUES
    ('d0000003-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0001-000000000003',
     'Breach Notification Window',
     'THRESHOLD', 'breach.hours_to_notify_authority', 'LESS_THAN', '72', 0, 1),

    ('d0000003-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0001-000000000003',
     'Data Retention Policy Documented',
     'PRESENCE', 'data.retention_policy_exists', 'EQUALS', 'true', 30, 2),

    ('d0000003-0000-0000-0000-000000000003',
     'c0000000-0000-0000-0001-000000000003',
     'DSAR Response Time',
     'THRESHOLD', 'dsar.response_days', 'LESS_THAN', '30', 0, 3),

    ('d0000003-0000-0000-0000-000000000004',
     'c0000000-0000-0000-0001-000000000003',
     'Consent Record Maintained',
     'PRESENCE', 'consent.record_exists', 'EQUALS', 'true', 0, 4),

    ('d0000003-0000-0000-0000-000000000005',
     'c0000000-0000-0000-0001-000000000003',
     'Privacy Impact Assessment for New Processing',
     'PRESENCE', 'pia.completed', 'EQUALS', 'true', 14, 5)
ON CONFLICT DO NOTHING;

-- ── HIPAA Rules ──────────────────────────────────────────────
INSERT INTO policy_rules (id, policy_id, name, rule_type, field, operator, value, grace_period_days, evaluation_order)
VALUES
    ('d0000004-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0001-000000000004',
     'PHI Access Logging',
     'PRESENCE', 'phi.access_logged', 'EQUALS', 'true', 0, 1),

    ('d0000004-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0001-000000000004',
     'BAA in Place for Business Associates',
     'PRESENCE', 'baa.signed', 'EQUALS', 'true', 30, 2),

    ('d0000004-0000-0000-0000-000000000003',
     'c0000000-0000-0000-0001-000000000004',
     'Minimum Necessary Access Principle',
     'PRESENCE', 'access.minimum_necessary_enforced', 'EQUALS', 'true', 0, 3),

    ('d0000004-0000-0000-0000-000000000004',
     'c0000000-0000-0000-0001-000000000004',
     'PHI Encryption at Rest',
     'PRESENCE', 'phi.encrypted_at_rest', 'EQUALS', 'true', 0, 4),

    ('d0000004-0000-0000-0000-000000000005',
     'c0000000-0000-0000-0001-000000000004',
     'HIPAA Training Completion',
     'FREQUENCY', 'hipaa_training.months_since_completed', 'LESS_THAN', '12', 0, 5)
ON CONFLICT DO NOTHING;

-- ── PCI DSS Rules ────────────────────────────────────────────
INSERT INTO policy_rules (id, policy_id, name, rule_type, field, operator, value, grace_period_days, evaluation_order)
VALUES
    ('d0000005-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0001-000000000005',
     'Cardholder Data Environment Segmented',
     'PRESENCE', 'network.cde_segmented', 'EQUALS', 'true', 0, 1),

    ('d0000005-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0001-000000000005',
     'PAN Never Stored Post-Authorization',
     'PRESENCE', 'data.pan_stored_post_auth', 'EQUALS', 'false', 0, 2),

    ('d0000005-0000-0000-0000-000000000003',
     'c0000000-0000-0000-0001-000000000005',
     'WAF Deployed',
     'PRESENCE', 'security.waf_enabled', 'EQUALS', 'true', 0, 3),

    ('d0000005-0000-0000-0000-000000000004',
     'c0000000-0000-0000-0001-000000000005',
     'Penetration Test Frequency',
     'FREQUENCY', 'pentest.months_since_last', 'LESS_THAN', '12', 0, 4),

    ('d0000005-0000-0000-0000-000000000005',
     'c0000000-0000-0000-0001-000000000005',
     'Anti-Malware on All Systems',
     'PRESENCE', 'security.antimalware_active', 'EQUALS', 'true', 0, 5)
ON CONFLICT DO NOTHING;

-- ── NIST CSF Rules ───────────────────────────────────────────
INSERT INTO policy_rules (id, policy_id, name, rule_type, field, operator, value, grace_period_days, evaluation_order)
VALUES
    ('d0000006-0000-0000-0000-000000000001',
     'c0000000-0000-0000-0001-000000000006',
     'Asset Inventory Maintained',
     'PRESENCE', 'asset.inventory_current', 'EQUALS', 'true', 7, 1),

    ('d0000006-0000-0000-0000-000000000002',
     'c0000000-0000-0000-0001-000000000006',
     'Threat Intelligence Feed Active',
     'PRESENCE', 'threat_intel.feed_active', 'EQUALS', 'true', 0, 2),

    ('d0000006-0000-0000-0000-000000000003',
     'c0000000-0000-0000-0001-000000000006',
     'Incident Response Plan Tested',
     'FREQUENCY', 'ir_plan.months_since_tested', 'LESS_THAN', '12', 0, 3),

    ('d0000006-0000-0000-0000-000000000004',
     'c0000000-0000-0000-0001-000000000006',
     'Recovery Time Objective Documented',
     'PRESENCE', 'bcp.rto_documented', 'EQUALS', 'true', 30, 4),

    ('d0000006-0000-0000-0000-000000000005',
     'c0000000-0000-0000-0001-000000000006',
     'Patch Cycle — Critical Vulnerabilities',
     'THRESHOLD', 'patching.critical_days_to_apply', 'LESS_THAN', '7', 0, 5)
ON CONFLICT DO NOTHING;

-- =============================================================
-- 5. DEFAULT NOTIFICATION CHANNEL (Email)
-- =============================================================
INSERT INTO notification_channels (
    id,
    organization_id,
    channel_type,
    name,
    config,
    min_severity,
    active
) VALUES (
    'e0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'EMAIL',
    'Default Email Alerts',
    -- config is encrypted at app layer in production;
    -- plain JSON here is safe only for dev/seed bootstrap
    '{"recipients": ["admin@company.com"], "subject_prefix": "[COMPLIANCE ALERT]"}',
    'HIGH',
    TRUE
) ON CONFLICT DO NOTHING;
