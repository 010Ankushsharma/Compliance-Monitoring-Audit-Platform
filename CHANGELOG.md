# Changelog

All notable changes to the Compliance Monitoring & Audit Platform are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---

## [Unreleased]

### Known Issues
- Missing `hypersistence-utils` dependency in `pom.xml` (won't compile until added)
- Policy evaluation engine not yet implemented — policies are stored and scheduled but rules are not executed
- TOTP MFA scaffold exists; full QR code + verification flow pending
- Kafka producer transactions disabled in dev profile to avoid startup error

---

## [1.0.0] — 2024-01-15

### Added

#### Core Platform
- Multi-tenant architecture with strict organisation-level data isolation
- 5-level RBAC: `SUPER_ADMIN`, `COMPLIANCE_OFFICER`, `AUDITOR`, `ANALYST`, `API_CLIENT`
- JWT authentication (HS512) with rotating refresh tokens
- BCrypt-12 password hashing
- Account lockout after 5 failed logins (30-minute window)
- TOTP MFA scaffold

#### Policy Management
- Full policy lifecycle: `DRAFT → ACTIVE → INACTIVE → ARCHIVED`
- State machine with validated transitions
- 6 regulatory framework policy types: ISO 27001, SOC 2, GDPR, HIPAA, PCI DSS v4.0, NIST CSF 2.0
- 5 rule types: THRESHOLD, PATTERN, PRESENCE, FREQUENCY, CUSTOM
- Per-rule grace period to suppress alert storms
- Cron-based evaluation schedule per policy
- Version tracking on every content change
- 30 pre-built seed rules across all 6 frameworks

#### Immutable Audit Trail
- SHA-256 hash-chained audit log (tamper-evident)
- Two-layer immutability: DB trigger + JPA lifecycle hook
- Full HTTP context captured per entry
- `/verify-chain` endpoint for integrity verification
- Configurable retention (default 2 years in production)
- Async Kafka-based publication

#### Violation Detection
- Workflow: `OPEN → IN_REVIEW → RESOLVED | FALSE_POSITIVE | SUPPRESSED`
- Domain methods enforce state transitions
- JSONB evidence snapshot at detection time
- Grace-period deduplication
- File-based evidence attachments (50 MB max)

#### Risk Scoring
- Weighted penalty algorithm (CRITICAL=40, HIGH=25, MEDIUM=15, LOW=5)
- Per-policy compliance score (0–100)
- Multiplicative amplifier for multiple same-severity violations
- Async recalculation after every violation status change
- PostgreSQL materialized view for instant dashboard reads

#### Alerts & Notifications
- Deduplication via deterministic `dedupKey`
- Severity-tiered auto-escalation (CRITICAL: 1h, HIGH: 4h, default: 24h)
- Email (JavaMailSender), Slack (rich blocks), webhook channels
- Notification batch dispatch every 30 seconds

#### Report Generation
- Fully async: PENDING → GENERATING → COMPLETED | FAILED
- Formats: PDF (iText 8), Excel (Apache POI), CSV, JSON
- Stuck-job recovery scheduler
- `GET /{id}/download` with content-type routing

#### Infrastructure
- Multi-stage Docker build (JDK builder + JRE Alpine runtime)
- 9-service Docker Compose with profiles (tools, monitoring)
- 10 Kubernetes manifests (Deployment, Service, Ingress, HPA, PDB, NetworkPolicy, RBAC, PVC)
- 4 GitHub Actions workflows (CI, CD, PR checks, scheduled nightly)
- Prometheus + Grafana monitoring stack
- Redis cache with per-region TTL configuration
- Apache Kafka with DLT handling and 3-retry error handling

#### API
- 50+ REST endpoints across 8 controllers
- OpenAPI 3 / Swagger UI
- Structured `ApiResponse<T>` / `PageResponse<T>` envelopes
- 15 exception handlers with no stack trace leaks
- Rate limiting (Bucket4j token-bucket per IP)

---

## Version History

| Version | Date | Highlights |
|---|---|---|
| 1.0.0 | 2024-01-15 | Initial release |

[Unreleased]: https://github.com/010Ankushsharma/Compliance-Monitoring-Audit-Platform/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/010Ankushsharma/Compliance-Monitoring-Audit-Platform/releases/tag/v1.0.0
