<div align="center">

# 🛡️ Compliance Monitoring & Audit Platform

**Enterprise-grade real-time compliance monitoring, immutable audit trail management,
policy enforcement, violation detection, and regulatory reporting.**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5-231F20?logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-ready-326CE5?logo=kubernetes&logoColor=white)](https://kubernetes.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

[Features](#-features) · [Architecture](#-architecture) · [Quick Start](#-quick-start) · [API Docs](#-api-documentation) · [Deployment](#-deployment) · [Contributing](#-contributing)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Database Schema](#-database-schema)
- [Security](#-security)
- [Regulatory Frameworks](#-regulatory-frameworks)
- [Kafka Event Flows](#-kafka-event-flows)
- [Caching Strategy](#-caching-strategy)
- [Deployment](#-deployment)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Monitoring & Observability](#-monitoring--observability)
- [Known Gaps & Roadmap](#-known-gaps--roadmap)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🌟 Overview

The **Compliance Monitoring & Audit Platform** is a production-ready Spring Boot application
designed for organisations that need to demonstrate and maintain compliance with multiple
regulatory frameworks simultaneously.

It provides:

- **Real-time policy evaluation** against ISO 27001, SOC 2, GDPR, HIPAA, PCI DSS v4.0, and NIST CSF 2.0
- **Immutable, hash-chained audit logs** with SHA-256 tamper detection — every action is recorded, nothing can be erased or altered
- **Automated violation detection** with severity-based risk scoring
- **Multi-channel alerting** (email, Slack, webhook) with deduplication and auto-escalation
- **Async report generation** in PDF, Excel, CSV, and JSON formats
- **Executive dashboard** with per-framework compliance scores and trend tracking
- **Multi-tenant architecture** — one platform serves multiple organisations with strict data isolation

---

## ✨ Features

### 🔐 Authentication & Authorization
- JWT access tokens (HS512, 1-hour TTL) + rotating refresh tokens (7-day TTL)
- 5-level RBAC: `SUPER_ADMIN`, `COMPLIANCE_OFFICER`, `AUDITOR`, `ANALYST`, `API_CLIENT`
- Account lockout after 5 failed logins (30-minute lockout window)
- TOTP-based MFA scaffold (full flow extensible)
- Refresh token rotation — old tokens revoked on use
- SHA-256 hashed refresh tokens — raw tokens never persisted

### 📋 Policy Management
- Full lifecycle: `DRAFT → ACTIVE → INACTIVE → ARCHIVED`
- State machine with validated transitions — illegal moves rejected
- Per-policy evaluation schedule (cron expression)
- Rule types: `THRESHOLD`, `PATTERN`, `PRESENCE`, `FREQUENCY`, `CUSTOM`
- Grace period per rule — prevents alert storms on known-good deviations
- Version tracking — every content change bumps the version counter
- Tag-based filtering and grouping

### 📜 Audit Trail (Immutable)
- **SHA-256 hash chain**: each entry hashes itself + the previous entry
- **Two-layer immutability**: DB trigger rejects `UPDATE`/`DELETE`, JPA `@PreUpdate`/`@PreRemove` throw `UnsupportedOperationException`
- Full HTTP context captured: method, endpoint, IP (X-Forwarded-For aware), User-Agent, request ID
- `/verify-chain` endpoint walks the entire chain and reports any broken links
- Configurable retention (default 2 years in production)
- Async publication via Kafka — zero latency impact on request threads

### 🚨 Violation Detection
- Workflow: `OPEN → IN_REVIEW → RESOLVED | FALSE_POSITIVE | SUPPRESSED`
- Domain methods enforce all state transitions (`violation.acknowledge()`, `violation.resolve()`)
- Evidence snapshot captured as JSONB at detection time
- Deduplication: grace-period window prevents duplicate violations for the same rule/user
- Risk score automatically recalculated after every status change
- File-based evidence attachments (up to 50 MB per file)

### 🔔 Alerts & Notifications
- Deduplication via deterministic `dedupKey` — no duplicate alert storms
- Severity-tiered auto-escalation: CRITICAL escalated after 1h, HIGH after 4h, others after 24h
- Multi-channel dispatch: Email (`JavaMailSender`), Slack (rich block payloads), generic webhook
- Notification batch dispatch every 30 seconds (scheduled)
- Per-channel minimum-severity filtering

### 📊 Risk Scoring
- Weighted penalty algorithm: CRITICAL=40pts, HIGH=25pts, MEDIUM=15pts, LOW=5pts per violation
- Multiplicative amplifier for multiple violations at the same severity
- Score range: 0 (non-compliant) to 100 (fully compliant)
- Per-policy scores aggregated into per-framework and overall organisation scores
- Materialized view (`compliance_score_snapshot`) for instant dashboard queries

### 📄 Report Generation
- Fully async: request creates a `PENDING` row → Kafka message → consumer generates → `COMPLETED`
- Formats: **PDF** (iText 8), **Excel** (Apache POI), **CSV**, **JSON**
- Pre-built templates for all 6 supported frameworks
- Stuck-job recovery scheduler (detects `GENERATING` jobs that never completed)
- Automatic file cleanup after configurable retention period
- `GET /download` returns correct `Content-Type`, `410 Gone` if file purged

### 🖥️ Executive Dashboard
- Single endpoint aggregates violations by severity, alerts, policy counts, per-framework scores
- Redis-cached for 2 minutes per organisation (avoids expensive multi-repo aggregation)
- Top 10 most critical open violations for immediate action
- Risk score trend indicator (IMPROVING / DECLINING / STABLE)

---

## 🛠 Tech Stack

| Category | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 17 LTS | Core language |
| Framework | Spring Boot | 3.2.5 | Application framework |
| Build | Maven | 3.9.x | Dependency management + build |
| Database | PostgreSQL | 15 | Primary datastore |
| Migrations | Flyway | 9.x | Schema versioning |
| Cache | Redis | 7 | Session cache, risk scores, dashboard |
| Messaging | Apache Kafka | 7.5 (Confluent) | Async audit events, violation events, report requests |
| ORM | Spring Data JPA + Hibernate | 6.x | Entity persistence |
| Security | Spring Security 6 + JJWT | 0.12.5 | JWT auth, RBAC |
| Rate Limiting | Bucket4j | 8.10 | Token-bucket per-IP rate limiting |
| Mapping | MapStruct | 1.5.5 | Compile-time entity↔DTO mapping |
| PDF | iText | 8.0.4 | PDF report generation |
| Excel | Apache POI | 5.2.5 | Excel report generation |
| Observability | Micrometer + Prometheus | — | Metrics export |
| Tracing | Micrometer Tracing (Brave) | — | Distributed trace context |
| API Docs | SpringDoc OpenAPI 3 | 2.5.0 | Swagger UI |
| Container | Docker + Docker Compose | — | Local dev + infra |
| Orchestration | Kubernetes + Kustomize | — | Production deployment |
| CI/CD | GitHub Actions | — | 4 workflows |

---

## 🏛 Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT / BROWSER                             │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTPS
                    ┌────────▼────────┐
                    │  NGINX Ingress   │  (TLS, rate limit, security headers)
                    └────────┬────────┘
                             │
              ┌──────────────▼──────────────┐
              │     Spring Boot API (x3)      │  Port 8080
              │                               │
              │  JwtAuthFilter → Controller   │
              │  RateLimitFilter → Service    │
              │  AuditAspect → Repository     │
              └──────┬──────────────┬────────┘
                     │              │
          ┌──────────▼──┐    ┌──────▼──────────┐
          │  PostgreSQL  │    │     Redis        │
          │  (primary)   │    │  (cache + RL)    │
          └─────────────┘    └─────────────────┘
                     │
          ┌──────────▼──────────────────────────┐
          │         Apache Kafka                  │
          │                                       │
          │  Topics:                              │
          │  • compliance.audit-events            │
          │  • compliance.violations              │
          │  • compliance.alerts                  │
          │  • compliance.report-requests         │
          │  • *.DLT (dead-letter topics)         │
          └──────────┬──────────────────────────┘
                     │
          ┌──────────▼──────────────────────────┐
          │         Kafka Consumers               │
          │                                       │
          │  AuditEventConsumer    (hash-chain)   │
          │  ViolationEventConsumer (alerts+risk) │
          │  ReportGenerationConsumer (PDF/Excel) │
          └─────────────────────────────────────┘
```

### Request Flow — Authenticated API Call

```
Client
  → [RateLimitFilter]     token-bucket check per IP
  → [JwtAuthFilter]       validate JWT, populate SecurityContext + MDC
  → [Controller]          validate request, call service
  → [AuditAspect]         @Around — capture context pre/post
  → [Service]             business logic, cache read/write
  → [Repository]          JPA → PostgreSQL
  → [AuditAspect]         publish AuditEvent to Kafka (async, fire-and-forget)
  → Controller returns response
```

### Audit Event Flow (Async)

```
AuditAspect
  → AuditLogService.publishAuditEvent()   [async / auditExecutor pool]
  → KafkaTemplate.send(audit-events)
  → AuditEventConsumer.consume()
  → AuditLogService.persistAuditLog()
      → compute SHA-256(previousHash + currentFields)
      → INSERT INTO audit_logs (immutable)
```

---

## 📁 Project Structure

```
compliance-audit-platform/
│
├── 📄 pom.xml                          Spring Boot 3.2.5 BOM, 40+ deps, 7 plugins
├── 📄 mvnw                             Maven wrapper (no Maven install required)
├── 📄 Dockerfile                       Multi-stage: JDK builder + JRE runtime (Alpine)
├── 📄 docker-compose.yml               9-service dev stack with Docker profiles
├── 📄 docker-compose.override.yml      Dev overrides (JDWP debugger, verbose SQL)
├── 📄 .env.example                     All environment variables with documentation
├── 📄 .gitignore
├── 📄 .commitlintrc.json               Conventional Commits enforcement
├── 📄 owasp-suppressions.xml           OWASP false-positive suppressions
│
├── 📁 .github/workflows/
│   ├── ci.yml                          validate → test → integration → security → build+push
│   ├── cd.yml                          staging (auto) + production (approval gate)
│   ├── pr-checks.yml                   Fast PR feedback (< 5 min)
│   └── scheduled.yml                   Nightly CVE scan + dependency updates
│
├── 📁 k8s/                             10 Kubernetes manifests
│   ├── kustomization.yaml              Single entry: kubectl apply -k k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml                     Placeholder Base64 values
│   ├── deployment.yaml                 3 replicas, anti-affinity, probes
│   ├── service.yaml                    ClusterIP + headless (Prometheus)
│   ├── ingress.yaml                    NGINX, TLS, cert-manager, rate limit
│   ├── hpa.yaml                        CPU + memory autoscaling (v2)
│   ├── pdb.yaml                        minAvailable: 2
│   ├── rbac.yaml                       ServiceAccount, Role, RoleBinding
│   └── network-policy.yaml             Default-deny + explicit allowlist
│
├── 📁 monitoring/
│   ├── prometheus.yml                  Scrape config (actuator/prometheus)
│   └── grafana/provisioning/           Auto-provisioned datasource + dashboards
│
├── 📁 scripts/
│   └── init-db.sh                      PG roles + extensions on first start
│
└── 📁 src/main/java/com/company/compliance/
    │
    ├── 📄 ComplianceAuditPlatformApplication.java
    │
    ├── 📁 annotation/
    │   ├── Auditable.java              @Auditable(action, resourceType, resourceIdArg)
    │   └── RequiresRole.java           @RequiresRole({"SUPER_ADMIN","AUDITOR"})
    │
    ├── 📁 config/
    │   ├── AppProperties.java          @ConfigurationProperties, startup validation
    │   ├── SecurityConfig.java         Spring Security 6, HSTS, CSP, CORS
    │   ├── KafkaConfig.java            Topics, producers, consumers, DLT, error handler
    │   ├── RedisConfig.java            Per-region TTL cache manager
    │   ├── AsyncConfig.java            3 thread pools (audit, report, general)
    │   ├── SwaggerConfig.java          OpenAPI 3, bearer auth, role table
    │   ├── JacksonConfig.java          ISO-8601 dates, null exclusion
    │   ├── AuditConfig.java            @EnableJpaAuditing
    │   └── WebClientConfig.java        RestTemplate bean
    │
    ├── 📁 domain/
    │   ├── enums/
    │   │   ├── Severity.java           CRITICAL(40) HIGH(25) MEDIUM(15) LOW(5) INFO(0)
    │   │   ├── PolicyStatus.java       DRAFT→ACTIVE→INACTIVE→ARCHIVED + canTransitionTo()
    │   │   ├── ViolationStatus.java    OPEN→IN_REVIEW→RESOLVED|FALSE_POSITIVE|SUPPRESSED
    │   │   └── RegulatoryFramework.java ISO_27001 SOC2 GDPR HIPAA PCI_DSS NIST CUSTOM
    │   └── entity/
    │       ├── Organization.java       Root tenant entity
    │       ├── User.java               RBAC roles, lockout logic
    │       ├── RefreshToken.java       SHA-256 hash stored, raw never persisted
    │       ├── Policy.java             State machine, rules cascade
    │       ├── PolicyRule.java         field + operator + value evaluation rule
    │       ├── AuditLog.java           IMMUTABLE — no setters, @PreUpdate throws
    │       ├── ComplianceViolation.java Domain methods: acknowledge/resolve/markFP
    │       ├── Alert.java              Dedup key, escalation, notification tracking
    │       ├── Report.java             markStarted/markCompleted/markFailed
    │       ├── PolicyEvaluation.java   Audit trail of each evaluation run
    │       ├── EvidenceAttachment.java Files attached to violations
    │       ├── RiskScore.java          Per-policy rolling compliance score
    │       └── NotificationChannel.java Per-org channel config (encrypted JSONB)
    │
    ├── 📁 dto/
    │   ├── common/                     ApiResponse<T>, PageResponse<T>, ErrorResponse
    │   ├── request/                    17 validated request DTOs
    │   └── response/                   14 response DTOs
    │
    ├── 📁 repository/                  8 repositories, 60+ custom queries
    │   ├── OrganizationRepository.java
    │   ├── UserRepository.java
    │   ├── RefreshTokenRepository.java
    │   ├── PolicyRepository.java       JpaSpecificationExecutor for dynamic filtering
    │   ├── AuditLogRepository.java     No @Modifying — insert+read only
    │   ├── ComplianceViolationRepository.java
    │   ├── AlertRepository.java
    │   └── ReportRepository.java
    │
    ├── 📁 security/
    │   ├── JwtTokenProvider.java       HS512 sign/verify, claims extraction
    │   ├── JwtAuthenticationFilter.java OncePerRequestFilter, MDC enrichment
    │   ├── CompliancePrincipal.java    Tenant-aware principal (no DB on each request)
    │   ├── CurrentUser.java            @AuthenticationPrincipal convenience annotation
    │   ├── ComplianceUserDetailsService.java Login only — JWT handles subsequent requests
    │   ├── AuditAspect.java            @Around @Auditable — async audit publication
    │   ├── RateLimitFilter.java        Bucket4j token-bucket per IP
    │   └── SecurityContextHelper.java  Static helper for service-layer access
    │
    ├── 📁 service/
    │   ├── AuthService.java            Login, token refresh, logout, session revocation
    │   ├── UserService.java            CRUD, password, MFA, lockout management
    │   ├── PolicyService.java          Lifecycle, state machine, cache eviction
    │   ├── AuditLogService.java        Dual-mode publish (Kafka/direct), chain verification
    │   ├── ViolationService.java       Status workflow, summary aggregation
    │   ├── AlertService.java           Creation, dispatch scheduler, escalation scheduler
    │   ├── RiskScoringService.java     Weighted penalty algorithm, async recalculation
    │   ├── NotificationService.java    Email + Slack + webhook dispatch
    │   └── ReportService.java          Async request, status polling, download URL
    │
    ├── 📁 kafka/
    │   ├── producer/
    │   │   ├── AuditEventProducer.java  Partition by orgId, Micrometer counters
    │   │   └── ViolationEventProducer.java Partition by orgId:policyId
    │   └── consumer/
    │       ├── AuditEventConsumer.java  MANUAL_IMMEDIATE ack, DLT handler
    │       ├── ViolationEventConsumer.java Alert creation + risk recalculation
    │       └── ReportGenerationConsumer.java PDF/Excel/CSV/JSON generation
    │
    ├── 📁 controller/
    │   ├── AuthController.java         /api/v1/auth/*
    │   ├── UserController.java         /api/v1/users/*
    │   ├── PolicyController.java       /api/v1/policies/*
    │   ├── AuditLogController.java     /api/v1/audit-logs/*
    │   ├── ViolationController.java    /api/v1/violations/*
    │   ├── AlertController.java        /api/v1/alerts/*
    │   ├── ReportController.java       /api/v1/reports/*
    │   └── DashboardController.java    /api/v1/dashboard/*
    │
    ├── 📁 exception/
    │   ├── GlobalExceptionHandler.java 15 handlers, no stack trace leaks
    │   ├── CompliancePlatformException.java Base with httpStatus + errorCode
    │   ├── ResourceNotFoundException.java   404
    │   ├── ConflictException.java           409
    │   ├── InvalidCredentialsException.java 401
    │   ├── AccountLockedException.java      401
    │   ├── InvalidTokenException.java       401
    │   ├── InvalidStateTransitionException.java 422
    │   ├── RateLimitExceededException.java  429
    │   └── ExternalServiceException.java    503
    │
    ├── 📁 mapper/                      8 MapStruct compile-time mappers
    │   ├── PolicyMapper.java           includes updateFromRequest() PATCH semantics
    │   ├── PolicyRuleMapper.java
    │   ├── UserMapper.java             passwordHash/mfaSecret NEVER in responses
    │   ├── AuditLogMapper.java         Read-only (no entity creation mapping)
    │   ├── ViolationMapper.java        Named converter for framework enum → String
    │   ├── AlertMapper.java
    │   ├── OrganizationMapper.java
    │   └── ReportMapper.java           downloadUrl ignored (set post-mapping)
    │
    └── 📁 event/                       4 Kafka event POJOs
        ├── AuditEvent.java
        ├── ViolationEvent.java         isNew flag for deduplication
        ├── AlertEvent.java
        └── ReportRequestEvent.java
```

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Install |
|---|---|---|
| Java JDK | 17+ | [adoptium.net](https://adoptium.net/) |
| Docker | 24+ | [docker.com](https://docs.docker.com/get-docker/) |
| Docker Compose | 2.20+ | Included with Docker Desktop |
| Git | Any | [git-scm.com](https://git-scm.com/) |

> Maven is **not required** — the included `mvnw` wrapper handles it.

---

### Option A — Docker Compose (Recommended for first run)

```bash
# 1. Clone the repository
git clone https://github.com/010Ankushsharma/Compliance-Monitoring-Audit-Platform.git
cd Compliance-Monitoring-Audit-Platform

# 2. Set up environment variables
cp .env.example .env

# 3. Generate a secure JWT secret (REQUIRED)
echo "JWT_SECRET=$(openssl rand -base64 64)" >> .env

# 4. Start the full stack (app + PostgreSQL + Redis + Kafka)
docker-compose up -d

# 5. Check all services are healthy
docker-compose ps

# 6. Follow application logs
docker-compose logs -f app
```

**Services available after startup:**

| Service | URL | Credentials |
|---|---|---|
| **API** | http://localhost:8080 | — |
| **Swagger UI** | http://localhost:8080/swagger-ui.html | — |
| **Health Check** | http://localhost:8081/actuator/health | — |
| **Kafka UI** | http://localhost:8090 | Start with `--profile tools` |
| **MailHog** | http://localhost:8025 | Start with `--profile tools` |
| **Prometheus** | http://localhost:9090 | Start with `--profile monitoring` |
| **Grafana** | http://localhost:3001 | admin / admin |

**Start with all dev tools:**
```bash
docker-compose --profile tools --profile monitoring up -d
```

---

### Option B — Local IDE Development

```bash
# Start only infrastructure (PostgreSQL, Redis, Kafka)
docker-compose up -d postgres redis kafka

# Run the application with Maven wrapper
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or with IDE: set Active Profile to "dev" and run ComplianceAuditPlatformApplication
```

---

### First Login

```bash
# Login with the seeded admin account
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@company.com",
    "password": "Admin@1234"
  }'

# Response includes accessToken — use it as Bearer token
export TOKEN="<accessToken from response>"

# Verify authentication
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

> ⚠️ **Change the default password immediately after first login!**

---

### Build the Project

```bash
# Compile + unit tests
./mvnw test

# Full build with integration tests
./mvnw verify -Pci

# Skip tests (emergency build only)
./mvnw package -DskipTests

# Check code style (Google Java Format)
./mvnw spotless:check

# Fix code style automatically
./mvnw spotless:apply

# OWASP vulnerability scan
./mvnw dependency-check:check -Pci
```

---

## ⚙️ Configuration

All configuration is driven by environment variables. Copy `.env.example` to `.env` and fill in your values.

### Required Variables

| Variable | Description | Example |
|---|---|---|
| `JWT_SECRET` | HS512 signing key — **min 64 chars in production** | `openssl rand -base64 64` |
| `DB_HOST` | PostgreSQL hostname | `localhost` |
| `DB_PASSWORD` | PostgreSQL password | `strong-password` |

### Key Optional Variables

| Variable | Default | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` or `prod` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Comma-separated Kafka brokers |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `EMAIL_NOTIFICATIONS_ENABLED` | `false` | Enable email notifications |
| `SLACK_WEBHOOK_URL` | _(empty)_ | Slack Incoming Webhook URL |
| `RATE_LIMIT_RPM` | `60` | Requests per minute per IP |
| `AUDIT_RETENTION_DAYS` | `730` | Days to retain audit logs |
| `REPORTS_STORAGE_PATH` | `/tmp/compliance-reports` | File storage path |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed origins |

See `.env.example` for the complete list with descriptions.

### Profile Differences

| Setting | `dev` | `prod` |
|---|---|---|
| SQL logging | Enabled (DEBUG) | Disabled |
| JWT expiry | 24 hours | 1 hour |
| Rate limiting | Disabled | Enabled |
| Async audit | Disabled (easier debug) | Enabled (Kafka) |
| SSL on PG/Redis | No | Required |
| Log format | Coloured console | Structured JSON |
| Kafka SASL | No auth | SASL_SSL |
| Kafka replication | 1 | 3 |

---

## 📖 API Documentation

### Interactive Docs

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

### API Endpoints

#### 🔐 Authentication — `/api/v1/auth`

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/login` | Public | Login, receive access + refresh tokens |
| `POST` | `/refresh` | Public | Rotate refresh token, get new access token |
| `POST` | `/logout` | Required | Revoke current refresh token |
| `POST` | `/logout-all` | Required | Revoke all sessions |
| `POST` | `/change-password` | Required | Change own password |
| `GET` | `/me` | Required | Get current user principal |

#### 👤 Users — `/api/v1/users`

| Method | Endpoint | Min Role | Description |
|---|---|---|---|
| `POST` | `/` | COMPLIANCE_OFFICER | Create user |
| `GET` | `/me` | Any | Own profile |
| `GET` | `/{id}` | Any | User by ID |
| `GET` | `/` | AUDITOR | List users in org |
| `PUT` | `/{id}` | Any (scoped) | Update profile |
| `POST` | `/me/change-password` | Any | Change own password |
| `POST` | `/{id}/change-password` | Any (scoped) | Change password by ID |
| `POST` | `/{id}/reset-password` | SUPER_ADMIN | Admin reset (no current PW) |
| `POST` | `/{id}/deactivate` | COMPLIANCE_OFFICER | Deactivate account |
| `POST` | `/{id}/activate` | COMPLIANCE_OFFICER | Activate + clear lockout |
| `POST` | `/{id}/unlock` | COMPLIANCE_OFFICER | Clear lockout only |
| `DELETE` | `/{id}` | SUPER_ADMIN | Soft-delete user |
| `POST` | `/{id}/mfa/disable` | Owner / SUPER_ADMIN | Disable MFA |

#### 📋 Policies — `/api/v1/policies`

| Method | Endpoint | Min Role | Description |
|---|---|---|---|
| `POST` | `/` | COMPLIANCE_OFFICER | Create policy |
| `GET` | `/` | Any | List policies (filtered) |
| `GET` | `/{id}` | Any | Get policy by ID |
| `PUT` | `/{id}` | COMPLIANCE_OFFICER | Update policy |
| `DELETE` | `/{id}` | COMPLIANCE_OFFICER | Soft-delete policy |
| `POST` | `/{id}/rules` | COMPLIANCE_OFFICER | Add rule to policy |
| `DELETE` | `/{id}/rules/{ruleId}` | COMPLIANCE_OFFICER | Remove rule |

#### 📜 Audit Logs — `/api/v1/audit-logs`

| Method | Endpoint | Min Role | Description |
|---|---|---|---|
| `GET` | `/` | AUDITOR | Search audit logs (filtered) |
| `GET` | `/{id}` | AUDITOR | Get log entry by ID |
| `GET` | `/verify-chain` | AUDITOR | Verify SHA-256 hash chain integrity |
| `GET` | `/user/{userId}` | AUDITOR | Get audit trail for a user |

#### 🚨 Violations — `/api/v1/violations`

| Method | Endpoint | Min Role | Description |
|---|---|---|---|
| `GET` | `/` | Any | List violations (filtered) |
| `GET` | `/{id}` | Any | Get violation by ID |
| `PATCH` | `/{id}/status` | AUDITOR | Update violation workflow status |
| `GET` | `/summary` | Any | Aggregated counts for dashboard |

#### 🔔 Alerts — `/api/v1/alerts`

| Method | Endpoint | Min Role | Description |
|---|---|---|---|
| `GET` | `/` | Any | List alerts (filtered) |
| `GET` | `/{id}` | Any | Get alert by ID |
| `POST` | `/{id}/acknowledge` | AUDITOR | Acknowledge alert |
| `POST` | `/{id}/resolve` | AUDITOR | Resolve alert |

#### 📄 Reports — `/api/v1/reports`

| Method | Endpoint | Min Role | Description |
|---|---|---|---|
| `POST` | `/` | AUDITOR | Request report generation |
| `GET` | `/` | Any | List reports |
| `GET` | `/{id}` | Any | Get report status |
| `GET` | `/{id}/download` | Any | Download completed report file |

#### 📊 Dashboard — `/api/v1/dashboard`

| Method | Endpoint | Min Role | Description |
|---|---|---|---|
| `GET` | `/` | Any | Executive compliance dashboard |
| `GET` | `/risk-scores` | Any | Per-policy risk scores |

### Standard Response Format

**Success:**
```json
{
  "success": true,
  "message": "Policy created successfully",
  "data": { ... },
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req-abc-123"
}
```

**Error:**
```json
{
  "status": 422,
  "error": "INVALID_STATE_TRANSITION",
  "message": "Cannot transition policy from ARCHIVED to ACTIVE",
  "path": "/api/v1/policies/123/",
  "timestamp": "2024-01-15T10:30:00Z",
  "requestId": "req-abc-123",
  "fieldErrors": {
    "status": ["Invalid status transition"]
  }
}
```

**Paginated list:**
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "last": false,
    "first": true,
    "empty": false
  }
}
```

---

## 🗄️ Database Schema

### Tables

| Table | Rows | Purpose |
|---|---|---|
| `organizations` | Multi-tenant root | Tenant isolation, risk score cache |
| `users` | Platform users | RBAC roles, lockout state, MFA secret |
| `refresh_tokens` | JWT refresh tokens | Token rotation, session revocation |
| `policies` | Compliance policies | Framework alignment, lifecycle state |
| `policy_rules` | Evaluation rules | field + operator + value per policy |
| `audit_logs` | **IMMUTABLE** | Hash-chained activity log |
| `compliance_violations` | Detected violations | Evidence JSONB, status workflow |
| `alerts` | Actionable alerts | Dedup key, escalation level |
| `reports` | Generated reports | Status, file path, summary JSONB |
| `report_frameworks` | Report↔framework | Many-to-many join |
| `notification_channels` | Channel config | Encrypted config JSONB |
| `risk_scores` | Per-policy scores | Rolling compliance score |
| `risk_score_history` | Score time-series | Trend charts |
| `policy_evaluations` | Evaluation runs | Scheduler audit trail |
| `evidence_attachments` | Violation files | Storage key, file metadata |

### Flyway Migrations

| Version | File | Contents |
|---|---|---|
| V1 | `V1__init_schema.sql` | All 11 tables, triggers, immutability guards, indexes |
| V2 | `V2__seed_frameworks.sql` | Default org, admin user, 6 framework policies, 30 rules |
| V3 | `V3__add_risk_scoring.sql` | Risk tables, evidence attachments, materialized view |

### Key Design Decisions

**Audit Log Immutability (two layers):**
```sql
-- DB trigger (V1)
CREATE TRIGGER trg_audit_logs_immutable_update
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_mutation();
```
```java
// JPA entity (@PreUpdate)
@PreUpdate
protected void rejectUpdate() {
    throw new UnsupportedOperationException("AuditLog entries are immutable");
}
```

**Hash Chain:**
```
entry_hash = SHA-256(id | timestamp | userId | action | resourceType | resourceId | outcome | previousHash)
```

**Materialized View (fast dashboard):**
```sql
CREATE MATERIALIZED VIEW compliance_score_snapshot AS
SELECT org.id, org.name, rs.framework,
       AVG(rs.compliance_score) AS avg_compliance_score,
       SUM(rs.open_violations_count) AS total_open_violations
FROM risk_scores rs
JOIN organizations org ON org.id = rs.organization_id
GROUP BY org.id, org.name, rs.framework;
```

---

## 🔒 Security

### Authentication Flow

```
1. POST /api/v1/auth/login  →  validate credentials  →  issue accessToken (JWT/HS512, 1h) + refreshToken (UUID, 7d)
2. All API calls            →  Authorization: Bearer <accessToken>
3. Token expiry             →  POST /api/v1/auth/refresh  →  rotate refreshToken  →  new accessToken
4. Logout                   →  POST /api/v1/auth/logout   →  revoke refreshToken(s)
```

### RBAC Matrix

| Action | SUPER_ADMIN | COMPLIANCE_OFFICER | AUDITOR | ANALYST | API_CLIENT |
|---|:-:|:-:|:-:|:-:|:-:|
| Manage any org | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create/edit policies | ✅ | ✅ (own org) | ❌ | ❌ | ❌ |
| Update violation status | ✅ | ✅ | ✅ | ❌ | ❌ |
| Read all resources | ✅ | ✅ | ✅ | ✅ | ✅ |
| Verify audit chain | ✅ | ✅ | ✅ | ❌ | ❌ |
| Generate reports | ✅ | ✅ | ✅ | ❌ | ❌ |
| Delete users | ✅ | ❌ | ❌ | ❌ | ❌ |
| Reset passwords | ✅ | ❌ | ❌ | ❌ | ❌ |

### Security Headers (production)

```
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; form-action 'self'
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
```

### Password Policy
- Minimum 12 characters
- Must contain: uppercase, lowercase, digit, special character (`@$!%*?&`)
- Stored as BCrypt (cost=12, ~250ms/hash — brute-force resistant)
- Current password required for self-service changes

### Rate Limiting
- Default: 60 requests/minute sustained, burst of 20
- Per IP address (respects `X-Forwarded-For`)
- Returns `429 Too Many Requests` with `Retry-After` header
- Configurable per environment (`RATE_LIMIT_RPM`, `RATE_LIMIT_BURST`)

---

## 📐 Regulatory Frameworks

| Framework | Code | Governing Body | Breach Notification |
|---|---|---|---|
| ISO/IEC 27001:2022 | `ISO_27001` | ISO | — |
| SOC 2 Type II | `SOC2` | AICPA | — |
| GDPR | `GDPR` | European Union | 72 hours |
| HIPAA | `HIPAA` | HHS (US) | 60 days |
| PCI DSS v4.0 | `PCI_DSS` | PCI SSC | 24 hours |
| NIST CSF 2.0 | `NIST` | NIST (US) | — |
| Custom | `CUSTOM` | Organisation-defined | — |

Each framework ships with **5 pre-built policy rules** covering real controls:

**Example — GDPR:**
- Breach notification within 72 hours
- Data retention policy documented
- DSAR response within 30 days
- Consent records maintained
- PIA completed for new processing activities

---

## 📨 Kafka Event Flows

### Topics

| Topic | Key | Producer | Consumers |
|---|---|---|---|
| `compliance.audit-events` | `organizationId` | `AuditEventProducer` | `AuditEventConsumer` |
| `compliance.violations` | `orgId:policyId` | `ViolationEventProducer` | `ViolationEventConsumer` |
| `compliance.alerts` | `organizationId` | `AlertService` | — |
| `compliance.report-requests` | `organizationId` | `ReportService` | `ReportGenerationConsumer` |
| `*.DLT` | Same as main | Error handler | DLT consumer (logs + metrics) |

### Reliability Settings

```yaml
Producer:
  acks: all                          # all ISR replicas confirm
  enable.idempotence: true           # exactly-once per partition
  retries: 3
  max.in.flight.requests.per.connection: 1

Consumer:
  enable.auto.commit: false          # manual acknowledgement
  isolation.level: read_committed    # only consume committed messages
  ack-mode: MANUAL_IMMEDIATE         # commit after successful processing
  concurrency: 3                     # matches partition count
  error-handler: 3 retries × 2s → DLT
```

---

## 🗃️ Caching Strategy

| Cache Name | TTL | Eviction Trigger | Content |
|---|---|---|---|
| `policies` | 10 min | Create / Update / Delete | Policy entities |
| `users` | 5 min | Update / Delete / Password change | User profiles |
| `organizations` | 30 min | Update | Org profiles |
| `risk-scores` | 15 min | After each evaluation | Per-policy scores |
| `dashboard` | 2 min | Any write to violations/alerts | Dashboard aggregation |
| `report-templates` | 6 hours | Static data | Template metadata |

All keys prefixed with `compliance:` in Redis.
Values serialised as JSON (not Java serialisation) — human-readable in `redis-cli`.

---

## 🚢 Deployment

### Docker (Single Machine)

```bash
# Production-like run with environment override
docker-compose \
  -f docker-compose.yml \
  --env-file .env.prod \
  up -d

# Scale the app (if not using K8s)
docker-compose up -d --scale app=3
```

### Kubernetes (Production)

```bash
# Prerequisites:
# - kubectl connected to your cluster
# - NGINX Ingress Controller installed
# - cert-manager installed (for TLS)
# - metrics-server installed (for HPA)

# 1. Create the namespace
kubectl apply -f k8s/namespace.yaml

# 2. Fill real values into secret.yaml (or use External Secrets)
# Replace placeholder Base64 values:
# echo -n "your-real-jwt-secret" | base64

# 3. Deploy everything with Kustomize
kubectl apply -k k8s/

# 4. Watch rollout
kubectl rollout status deployment/compliance-app -n compliance

# 5. Check pods
kubectl get pods -n compliance

# 6. View logs
kubectl logs -l app=compliance-app -n compliance --tail=100 -f
```

**External Secrets (recommended for production):**
```yaml
# Instead of k8s/secret.yaml, use AWS Secrets Manager:
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: compliance-secrets
spec:
  secretStoreRef:
    name: aws-secretsmanager
    kind: ClusterSecretStore
  target:
    name: compliance-secrets
  data:
    - secretKey: JWT_SECRET
      remoteRef:
        key: compliance-platform/prod
        property: jwt_secret
```

### Infrastructure Requirements (Production)

| Component | Recommended | Minimum |
|---|---|---|
| App pods | 3 (across 3 AZs) | 2 |
| App memory | 1 GB limit / 512 MB request | 512 MB |
| App CPU | 1000m limit / 250m request | 250m |
| PostgreSQL | RDS Multi-AZ (db.r6g.large) | Single instance |
| Redis | ElastiCache (cache.r6g.large) | Single node |
| Kafka | MSK 3-broker cluster | Single broker |
| Storage | EFS (ReadWriteMany, 20 GB) | Local disk |

---

## 🔄 CI/CD Pipeline

### Workflow Overview

```
Push to main / PR opened
        │
        ▼
┌───────────────────┐
│  pr-checks.yml    │  (PRs only — < 5 min)
│  • Commit lint    │
│  • Style check    │
│  • Compile        │
│  • Unit tests     │
│  • Dep review     │
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  ci.yml           │  (Push to main/develop)
│  • Validate       │
│  • Unit tests     │──── JaCoCo ≥ 80%
│  • Integration    │──── Testcontainers
│  • OWASP scan     │──── CVSS ≥ 7 fails
│  • Trivy scan     │──── Critical/High fails
│  • Build + push   │──── GHCR (multi-arch)
└───────────────────┘
        │  (main only, CI passed)
        ▼
┌───────────────────┐
│  cd.yml           │
│                   │
│  1. Staging       │──── Auto-deploy + smoke tests
│     └─ pass       │
│  2. Production    │──── Requires human approval
│     └─ pass       │──── Creates GitHub release
│     └─ fail       │──── Auto-rollback
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  scheduled.yml    │  (02:00 UTC nightly)
│  • Full CVE scan  │
│  • Dep updates    │
└───────────────────┘
```

### GitHub Secrets Required

| Secret | Description |
|---|---|
| `NVD_API_KEY` | NVD API key for OWASP dep-check (avoids rate limits) |
| `AWS_ROLE_ARN_STAGING` | IAM role ARN for EKS staging (OIDC) |
| `AWS_ROLE_ARN_PROD` | IAM role ARN for EKS production (OIDC) |
| `AWS_REGION` | AWS region (e.g. `us-east-1`) |
| `EKS_CLUSTER_NAME_STAGING` | EKS cluster name for staging |
| `EKS_CLUSTER_NAME_PROD` | EKS cluster name for production |
| `SLACK_DEPLOY_WEBHOOK` | Slack webhook URL for deploy notifications |

### Commit Message Format (enforced)

```
type(scope): subject

Examples:
feat(policy): add evaluation schedule cron validation
fix(auth): clear lockout counter on successful login
security(audit): add SHA-256 chain verification endpoint
ci(k8s): add network policy for egress to Kafka
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `chore`, `revert`, `security`

**Scopes:** `auth`, `policy`, `audit`, `violation`, `alert`, `report`, `dashboard`, `kafka`, `security`, `config`, `docker`, `k8s`, `ci`, `deps`

---

## 📈 Monitoring & Observability

### Metrics (Prometheus)

Key metrics exported via `/actuator/prometheus` (port 8081):

| Metric | Type | Description |
|---|---|---|
| `audit.events.published.total` | Counter | Audit events sent to Kafka |
| `audit.events.failed.total` | Counter | Failed Kafka publishes |
| `audit.events.consumed.total` | Counter | Events persisted by consumer |
| `audit.events.dlt.total` | Counter | Events routed to DLT |
| `violations.events.consumed.total` | Counter | Violation events processed |
| `reports.generated.total` | Counter | Reports successfully generated |
| `reports.generation.duration` | Timer | Report generation time (p50/p95/p99) |
| `http.server.requests` | Timer | API request latency histogram |

### Distributed Tracing

All requests get a `traceId` + `spanId` in:
- MDC (appears in every log line)
- Kafka message headers (`X-Trace-Id`, `X-Span-Id`)
- Response (via Micrometer Brave → Zipkin)

### Structured Logging (Production)

```json
{
  "timestamp": "2024-01-15T10:30:00.000+0000",
  "level": "INFO",
  "service": "compliance-audit-platform",
  "traceId": "abc123",
  "spanId": "def456",
  "thread": "compliance-async-1",
  "logger": "PolicyService",
  "message": "Policy 'GDPR Data Retention' created [id=uuid] by user admin@company.com"
}
```

### Health Endpoints (port 8081)

```bash
# Overall health
GET /actuator/health

# Kubernetes liveness probe
GET /actuator/health/liveness

# Kubernetes readiness probe
GET /actuator/health/readiness

# Prometheus metrics
GET /actuator/prometheus

# Flyway migration status
GET /actuator/flyway

# Cache statistics
GET /actuator/caches
```

---

## 🗺️ Known Gaps & Roadmap

### Current Gaps (see full analysis in project notes)

| Gap | Impact | Fix Effort |
|---|---|---|
| Missing `hypersistence-utils` in `pom.xml` | Won't compile | 5 min |
| No test coverage (1 smoke test only) | CI coverage check fails | 1–2 weeks |
| Policy evaluation engine missing | Core detection doesn't run | 3–5 days |
| MFA full flow (QR code + TOTP verify) | Scaffold only | 2–3 days |
| Kafka dev/prod transaction config mismatch | Dev startup error | 10 min |

### Roadmap

**v1.1 — Stability**
- [ ] Fix compilation issues (`hypersistence-utils`, Kafka dev config)
- [ ] Unit tests for all services (target ≥ 80% coverage)
- [ ] Integration tests with Testcontainers
- [ ] Policy evaluation engine

**v1.2 — Features**
- [ ] Full TOTP MFA flow with QR code generation
- [ ] Rich report templates (SOC 2 Type II evidence package, GDPR report)
- [ ] Webhook notification channel
- [ ] PagerDuty integration
- [ ] CSV export for audit logs

**v1.3 — Scale**
- [ ] Redis-backed Bucket4j (multi-node rate limiting)
- [ ] Read replicas for audit log queries
- [ ] S3/GCS report storage
- [ ] KEDA-based Kafka consumer autoscaling

**v2.0 — Enterprise**
- [ ] SAML/SSO integration
- [ ] Custom report builder
- [ ] API key management (long-lived `API_CLIENT` tokens)
- [ ] Compliance calendar / evidence collection scheduler
- [ ] Audit log export to SIEM (Splunk, Elastic SIEM)

---

## 🤝 Contributing

### Development Setup

```bash
# Fork and clone
git clone https://github.com/<your-fork>/Compliance-Monitoring-Audit-Platform.git
cd Compliance-Monitoring-Audit-Platform

# Install Git hooks (commit lint)
npm install --save-dev @commitlint/cli @commitlint/config-conventional

# Start infra
docker-compose up -d postgres redis kafka

# Run in dev mode
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Pull Request Checklist

- [ ] Commit messages follow Conventional Commits (`feat(scope): subject`)
- [ ] Code passes `./mvnw spotless:check` (Google Java Format)
- [ ] New service methods have unit tests
- [ ] New endpoints have integration tests
- [ ] Sensitive fields (passwords, secrets) excluded from all DTOs
- [ ] `@Auditable` annotation added to state-changing service methods
- [ ] Cache evicted on all write operations
- [ ] Multi-tenant isolation checked (principal.canManage(orgId))
- [ ] PR description explains the "why", not just the "what"

### Branch Strategy

```
main          ← production releases (protected, requires PR + approval)
develop       ← integration branch (protected, requires PR)
feature/*     ← new features (branch from develop)
fix/*         ← bug fixes (branch from main or develop)
security/*    ← security patches (branch from main, expedited review)
```

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

```
MIT License — Copyright (c) 2024 Ankush Sharma

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software.
```

---

## 🙏 Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot) — Application framework
- [Apache Kafka](https://kafka.apache.org/) — Event streaming
- [iText](https://itextpdf.com/) — PDF generation
- [Apache POI](https://poi.apache.org/) — Excel generation
- [MapStruct](https://mapstruct.org/) — Compile-time mapping
- [Bucket4j](https://bucket4j.com/) — Rate limiting
- [SpringDoc](https://springdoc.org/) — OpenAPI documentation

---

<div align="center">

**Built with ❤️ for enterprise compliance teams**

[⬆ Back to top](#-compliance-monitoring--audit-platform)

</div>