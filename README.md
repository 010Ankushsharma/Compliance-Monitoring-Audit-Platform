# 🛡️ Compliance Monitoring & Audit Platform
 
[![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Build](https://img.shields.io/badge/Build-Maven-red?style=flat-square&logo=apachemaven)](https://maven.apache.org/)
    
A robust, enterprise-grade **Compliance Monitoring & Audit Platform** built with Java Spring Boot. The platform enables organizations to define compliance policies, monitor system and user activities in real-time, generate audit trails, detect violations, and produce regulatory reports — all through a secure, scalable REST API.

---
## 📋 Table of Contents
 
- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
  - [Running the Application](#running-the-application)
- [API Reference](#-api-reference)
  - [Authentication](#authentication)
  - [Compliance Policies](#compliance-policies)
  - [Audit Logs](#audit-logs)
  - [Monitoring & Alerts](#monitoring--alerts)
  - [Reports](#reports)
- [Database Schema](#-database-schema)
- [Security](#-security)
- [Event-Driven Architecture](#-event-driven-architecture)
- [Testing](#-testing)
- [Deployment](#-deployment)
  - [Docker](#docker)
  - [Kubernetes](#kubernetes)
- [Environment Variables](#-environment-variables)
- [Roadmap](#-roadmap)
- [Contributing](#-contributing)
- [License](#-license)   
---

## 🌐 Overview
 
Organizations operating in regulated industries (finance, healthcare, government, etc.) must continuously monitor their systems, enforce internal policies, and produce accurate audit trails for regulatory bodies. Manual processes are error-prone and slow.
 
The **Compliance Monitoring & Audit Platform** automates this entire lifecycle:
 
1. **Define** compliance rules and control frameworks (e.g., ISO 27001, SOC 2, GDPR, HIPAA)
2. **Monitor** system events, user actions, and data access in real-time
3. **Detect** policy violations and anomalous behavior automatically
4. **Audit** — maintain immutable, tamper-evident logs of all activity
5. **Report** — generate compliance evidence reports for auditors and executives
---   




 ## ✨ Features   
 
### Core Compliance Engine
- Policy definition with rule-based and threshold-based compliance checks
- Support for multiple regulatory frameworks simultaneously
- Custom control mapping and risk scoring
- Evidence collection and attachment management   
### Audit Trail
- Immutable event log with cryptographic hash chaining  
- Full request/response capture with user context
- Data access auditing (who accessed what and when)
- Configurable retention policies per data classification
### Real-Time Monitoring
- Event stream processing for instant violation detection
- Configurable alert thresholds and escalation paths
- Dashboard metrics with live compliance posture scoring
- Scheduled compliance assessments  
### Alerting & Notifications
- Multi-channel notifications (email, Slack, webhook, SMS)
- Severity-based routing (INFO, WARNING, CRITICAL)
- Alert deduplication and suppression rules   
- On-call escalation support
### Reporting & Analytics
- Pre-built report templates for ISO 27001, SOC 2, GDPR, HIPAA
- Custom report builder with filters and date ranges
- Exportable to PDF, Excel, and CSV
- Executive dashboards with trend analysis
### Security
- JWT-based authentication with refresh token rotation
- Role-based access control (RBAC) with fine-grained permissions
- OAuth 2.0 / OIDC integration (SSO)
- API rate limiting and IP allowlisting
- Encrypted data at rest and in transit
---
## 🏗️ Architecture
 
```
┌───────────────────────────────────────────────────────────────────┐
│                         Client Applications                       │
│              (Web UI / Mobile App / Third-Party Systems)          │
└────────────────────────────┬──────────────────────────────────────┘
                             │ HTTPS / REST
┌────────────────────────────▼──────────────────────────────────────┐
│                        API Gateway Layer                          │
│               (Spring Security + Rate Limiter + JWT)              │
└──────┬──────────────┬───────────────┬───────────────┬─────────────┘
       │              │               │               │
┌──────▼────┐  ┌──────▼────┐  ┌──────▼────┐  ┌──────▼────────┐
│  Policy   │  │  Audit    │  │ Monitor   │  │  Report       │
│  Service  │  │  Service  │  │ Service   │  │  Service      │
└──────┬────┘  └──────┬────┘  └──────┬────┘  └──────┬────────┘
       │              │               │               │
┌──────▼──────────────▼───────────────▼───────────────▼──────────┐
│                      Domain & Business Logic Layer               │
│               (Violation Engine / Risk Scoring / Rules)         │
└──────────────────────────────┬──────────────────────────────────┘
                               │
       ┌───────────────────────┼───────────────────────┐
       │                       │                       │
┌──────▼──────┐        ┌───────▼──────┐        ┌──────▼──────┐
│  PostgreSQL │        │  Apache      │        │   Redis     │
│  (Primary   │        │  Kafka       │        │  (Cache +   │
│   Storage)  │        │  (Events)    │        │   Sessions) │
└─────────────┘        └──────────────┘        └─────────────┘
```
 
The platform follows a **layered architecture** with clear separation between API, service, and persistence concerns:
 
- **Controller Layer** — REST endpoints, request validation, response mapping
- **Service Layer** — Business logic, orchestration, rule evaluation
- **Repository Layer** — JPA/Hibernate data access with Spring Data
- **Event Layer** — Kafka producers/consumers for async processing
- **Security Layer** — JWT filter chain, RBAC enforcement
---
## 🛠️ Tech Stack
 
| Category | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security 6, JWT, OAuth2 |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Caching | Redis (Spring Cache) |
| Messaging | Apache Kafka |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Build Tool | Apache Maven |
| Testing | JUnit 5, Mockito, Testcontainers |
| Containerization | Docker, Docker Compose |
| Monitoring | Spring Actuator, Micrometer, Prometheus |
| Logging | SLF4J + Logback, structured JSON logs |
 
---


## 📁 Project Structure
 
```
compliance-audit-platform/
│
├── src/
│   ├── main/
│   │   ├── java/com/company/compliance/
│   │   │   │
│   │   │   ├── ComplianceAuditApplication.java       # Entry point
│   │   │   │
│   │   │   ├── config/                               # App configuration
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── KafkaConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── SwaggerConfig.java
│   │   │   │   └── AuditConfig.java
│   │   │   │
│   │   │   ├── controller/                           # REST controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── PolicyController.java
│   │   │   │   ├── AuditLogController.java
│   │   │   │   ├── MonitoringController.java
│   │   │   │   ├── AlertController.java
│   │   │   │   ├── ReportController.java
│   │   │   │   └── DashboardController.java
│   │   │   │
│   │   │   ├── service/                              # Business logic
│   │   │   │   ├── PolicyService.java
│   │   │   │   ├── AuditLogService.java
│   │   │   │   ├── ViolationDetectionService.java
│   │   │   │   ├── AlertService.java
│   │   │   │   ├── ReportGenerationService.java
│   │   │   │   ├── RiskScoringService.java
│   │   │   │   └── NotificationService.java
│   │   │   │
│   │   │   ├── domain/                               # Domain models
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Policy.java
│   │   │   │   │   ├── PolicyRule.java
│   │   │   │   │   ├── AuditLog.java
│   │   │   │   │   ├── ComplianceViolation.java
│   │   │   │   │   ├── Alert.java
│   │   │   │   │   ├── Report.java
│   │   │   │   │   ├── User.java
│   │   │   │   │   └── Organization.java
│   │   │   │   ├── enums/
│   │   │   │   │   ├── Severity.java
│   │   │   │   │   ├── PolicyStatus.java
│   │   │   │   │   ├── ViolationStatus.java
│   │   │   │   │   └── RegulatoryFramework.java
│   │   │   │   └── event/
│   │   │   │       ├── AuditEvent.java
│   │   │   │       └── ViolationEvent.java
│   │   │   │
│   │   │   ├── repository/                           # Data access
│   │   │   │   ├── PolicyRepository.java
│   │   │   │   ├── AuditLogRepository.java
│   │   │   │   ├── ViolationRepository.java
│   │   │   │   ├── AlertRepository.java
│   │   │   │   └── ReportRepository.java
│   │   │   │
│   │   │   ├── kafka/                                # Kafka producers/consumers
│   │   │   │   ├── producer/
│   │   │   │   │   ├── AuditEventProducer.java
│   │   │   │   │   └── ViolationEventProducer.java
│   │   │   │   └── consumer/
│   │   │   │       ├── AuditEventConsumer.java
│   │   │   │       └── ViolationEventConsumer.java
│   │   │   │
│   │   │   ├── security/                             # Auth & security
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── UserDetailsServiceImpl.java
│   │   │   │   └── AuditAspect.java                 # AOP audit interceptor
│   │   │   │
│   │   │   ├── dto/                                  # Request/Response DTOs
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   │
│   │   │   ├── mapper/                               # Entity ↔ DTO mappers
│   │   │   │
│   │   │   └── exception/                            # Error handling
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── PolicyNotFoundException.java
│   │   │       └── ViolationException.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/                         # Flyway SQL scripts
│   │           ├── V1__init_schema.sql
│   │           ├── V2__seed_frameworks.sql
│   │           └── V3__add_risk_scoring.sql
│   │
│   └── test/
│       └── java/com/company/compliance/
│           ├── controller/
│           ├── service/
│           └── integration/
│
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
│
├── k8s/
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── configmap.yaml
│   └── ingress.yaml
│
├── docs/
│   ├── api-reference.md
│   └── architecture.md
│
├── pom.xml
├── .env.example
├── .gitignore
└── README.md
```
 
---
## 🚀 Getting Started
 
### Prerequisites
 
Ensure the following are installed on your machine:
 
- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.8+** — [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** — [Download](https://www.docker.com/get-started)
- **PostgreSQL 15** (or run via Docker)
- **Redis 7** (or run via Docker)
- **Apache Kafka** (or run via Docker)
### Installation
 
**1. Clone the repository**
 
```bash
git clone https://github.com/your-org/compliance-audit-platform.git
cd compliance-audit-platform
```
 
**2. Copy the environment file**
 
```bash
cp .env.example .env
# Edit .env with your configuration values
```
 
**3. Start infrastructure services with Docker Compose**
 
```bash
docker-compose up -d postgres redis kafka zookeeper
```
 
**4. Build the application**
 
```bash
mvn clean install -DskipTests
```
 
### Configuration
 
Edit `src/main/resources/application.yml` or provide values through environment variables:
 
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/compliance_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: compliance-platform
      auto-offset-reset: earliest
 
app:
  jwt:
    secret: ${JWT_SECRET}
    expiration-ms: 3600000         # 1 hour
    refresh-expiration-ms: 604800000  # 7 days
  audit:
    retention-days: 365
    hash-algorithm: SHA-256
  notifications:
    email:
      smtp-host: ${SMTP_HOST}
      smtp-port: ${SMTP_PORT}
      from: noreply@yourcompany.com
    slack:
      webhook-url: ${SLACK_WEBHOOK_URL}
```
 
### Running the Application
 
**Development mode:**
 
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
 
**Production mode:**
 
```bash
java -jar target/compliance-audit-platform-1.0.0.jar --spring.profiles.active=prod
```
 
The API will be available at: `http://localhost:8080`
 
Swagger UI: `http://localhost:8080/swagger-ui.html`
 
Actuator: `http://localhost:8080/actuator`
 
---


## 📡 API Reference
 
All endpoints are prefixed with `/api/v1`. Full interactive documentation is available via Swagger UI once the app is running.
 
### Authentication
 
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/auth/register` | Register a new user |
| `POST` | `/auth/login` | Authenticate and receive JWT |
| `POST` | `/auth/refresh` | Refresh access token |
| `POST` | `/auth/logout` | Invalidate refresh token |
 
**Login example:**
 
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin@company.com", "password": "yourpassword"}'
```
 
**Response:**
 
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```
 
---
 
### Compliance Policies
 
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/policies` | List all policies (paginated) |
| `POST` | `/policies` | Create a new compliance policy |
| `GET` | `/policies/{id}` | Get policy details |
| `PUT` | `/policies/{id}` | Update a policy |
| `DELETE` | `/policies/{id}` | Deactivate a policy |
| `POST` | `/policies/{id}/rules` | Add a rule to a policy |
| `GET` | `/policies/{id}/violations` | Get violations for a policy |
| `POST` | `/policies/{id}/evaluate` | Manually trigger policy evaluation |
 
**Create policy example:**
 
```bash
curl -X POST http://localhost:8080/api/v1/policies \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MFA Enforcement Policy",
    "framework": "ISO_27001",
    "description": "All users must have MFA enabled within 7 days of account creation",
    "severity": "HIGH",
    "rules": [
      {
        "type": "THRESHOLD",
        "field": "mfa_enabled",
        "operator": "EQUALS",
        "value": "true",
        "gracePeriodDays": 7
      }
    ]
  }'
```
 
---
 
### Audit Logs
 
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/audit-logs` | Query audit logs with filters |
| `GET` | `/audit-logs/{id}` | Get a specific audit log entry |
| `GET` | `/audit-logs/user/{userId}` | Get all logs for a user |
| `GET` | `/audit-logs/resource/{resourceId}` | Get logs for a specific resource |
| `GET` | `/audit-logs/export` | Export logs as CSV/JSON |
| `POST` | `/audit-logs/verify` | Verify log chain integrity |
 
**Query audit logs example:**
 
```bash
curl "http://localhost:8080/api/v1/audit-logs?userId=123&startDate=2024-01-01&endDate=2024-06-01&action=DATA_ACCESS&page=0&size=20" \
  -H "Authorization: Bearer <token>"
```
 
**Response:**
 
```json
{
  "content": [
    {
      "id": "a1b2c3d4",
      "timestamp": "2024-03-15T14:22:10Z",
      "userId": "123",
      "userName": "john.doe@company.com",
      "action": "DATA_ACCESS",
      "resourceType": "CUSTOMER_RECORD",
      "resourceId": "cust-789",
      "ipAddress": "10.0.1.55",
      "outcome": "SUCCESS",
      "hash": "sha256:3a7f2b...",
      "previousHash": "sha256:1c4d8e..."
    }
  ],
  "totalElements": 142,
  "totalPages": 8
}
```
 
---
 
### Monitoring & Alerts
 
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/violations` | List compliance violations |
| `GET` | `/violations/{id}` | Get violation details |
| `PATCH` | `/violations/{id}/status` | Update violation status (OPEN/IN_REVIEW/RESOLVED) |
| `GET` | `/alerts` | List active alerts |
| `GET` | `/alerts/{id}` | Get alert details |
| `POST` | `/alerts/{id}/acknowledge` | Acknowledge an alert |
| `POST` | `/alerts/{id}/resolve` | Resolve an alert |
| `GET` | `/dashboard/metrics` | Real-time compliance posture metrics |
 
**Dashboard metrics response example:**
 
```json
{
  "complianceScore": 87.4,
  "openViolations": 12,
  "criticalAlerts": 2,
  "policiesActive": 34,
  "eventsLast24h": 15823,
  "topRiskyUsers": [...],
  "frameworkScores": {
    "ISO_27001": 91.2,
    "SOC2": 84.6,
    "GDPR": 86.0
  }
}
```
 
---
 
### Reports
 
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/reports` | List generated reports |
| `POST` | `/reports/generate` | Generate a new compliance report |
| `GET` | `/reports/{id}` | Get report details |
| `GET` | `/reports/{id}/download` | Download report (PDF/Excel/CSV) |
| `GET` | `/reports/templates` | List available report templates |
 
**Generate report example:**
 
```bash
curl -X POST http://localhost:8080/api/v1/reports/generate \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "templateId": "soc2-type2",
    "startDate": "2024-01-01",
    "endDate": "2024-06-30",
    "format": "PDF",
    "includeEvidence": true,
    "frameworks": ["SOC2", "ISO_27001"]
  }'
```
 
---
 
## 🗄️ Database Schema
 
Key tables in the PostgreSQL schema:
 
```sql
-- Core entities
policies           (id, name, framework, severity, status, created_at, updated_at)
policy_rules       (id, policy_id, type, field, operator, value, grace_period_days)
audit_logs         (id, timestamp, user_id, action, resource_type, resource_id, 
                    outcome, ip_address, hash, previous_hash)
compliance_violations (id, policy_id, user_id, detected_at, severity, status, 
                       evidence, resolved_at)
alerts             (id, violation_id, severity, message, status, 
                    created_at, acknowledged_by, resolved_at)
reports            (id, template_id, generated_by, start_date, end_date, 
                    status, file_path, created_at)
users              (id, email, name, role, mfa_enabled, created_at)
organizations      (id, name, industry, regulatory_frameworks)
```
 
Database migrations are managed by **Flyway** and run automatically on startup.
 
---
 
## 🔒 Security
 
### Authentication Flow
 
```
Client → POST /auth/login → JWT Access Token (1hr) + Refresh Token (7d)
Client → Request with Bearer Token → JwtAuthenticationFilter validates → SecurityContext set
Client → POST /auth/refresh → New Access Token issued
```
 
### Roles & Permissions
 
| Role | Permissions |
|---|---|
| `SUPER_ADMIN` | Full access — manage users, all policies, all reports |
| `COMPLIANCE_OFFICER` | Create/edit policies, view all violations, generate reports |
| `AUDITOR` | Read-only access to audit logs, violations, and reports |
| `ANALYST` | View dashboards, metrics, and filtered audit data |
| `API_CLIENT` | Programmatic access to emit audit events |
 
### AOP Audit Interceptor
 
Every API call is automatically captured by `AuditAspect.java` using Spring AOP:
 
```java
@Aspect
@Component
public class AuditAspect {
 
    @Around("@annotation(Auditable)")
    public Object captureAuditEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        // Extracts user context, request details, and outcome
        // Publishes immutable AuditEvent to Kafka
        // Computes and chains SHA-256 hashes
    }
}
```
 
Annotate any service method with `@Auditable` to enable automatic audit capture.
 
---
 
## ⚡ Event-Driven Architecture
 
The platform uses **Apache Kafka** for asynchronous event processing:
 
| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `compliance.audit-events` | API Layer | AuditLogService | Persist all audit events |
| `compliance.violations` | ViolationDetectionService | AlertService | Trigger alerts on violations |
| `compliance.alerts` | AlertService | NotificationService | Send notifications |
| `compliance.report-requests` | ReportController | ReportGenerationService | Async report generation |
 
**Kafka consumer group:** `compliance-platform`
 
This design ensures the audit trail write path is non-blocking and fault-tolerant.
 
---
 
## 🧪 Testing
 
The project includes unit tests, service-layer tests, and full integration tests using Testcontainers.
 
**Run all tests:**
 
```bash
mvn test
```
 
**Run only unit tests:**
 
```bash
mvn test -Dgroups="unit"
```
 
**Run integration tests (requires Docker):**
 
```bash
mvn verify -Dgroups="integration"
```
 
**Generate test coverage report:**
 
```bash
mvn jacoco:report
# Report at target/site/jacoco/index.html
```
 
Target coverage: **≥ 80%** on service and domain layers.
 
---
 
## 🐳 Deployment
 
### Docker
 
**Build the image:**
 
```bash
docker build -t compliance-audit-platform:latest .
```
 
**Run with Docker Compose (full stack):**
 
```bash
docker-compose up --build
```
 
The `docker-compose.yml` includes: the application, PostgreSQL, Redis, Kafka, Zookeeper, and a Kafka UI for development.
 
**`docker-compose.yml` excerpt:**
 
```yaml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=postgres
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - postgres
      - redis
      - kafka
 
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: compliance_db
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
 
  redis:
    image: redis:7-alpine
 
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on: [zookeeper]
```
 
### Kubernetes
 
```bash
# Apply all manifests
kubectl apply -f k8s/
 
# Check deployment
kubectl get pods -n compliance
 
# View logs
kubectl logs -f deployment/compliance-platform -n compliance
```
 
---
 
## 🌍 Environment Variables
 
| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_HOST` | Yes | `localhost` | PostgreSQL host |
| `DB_PORT` | No | `5432` | PostgreSQL port |
| `DB_NAME` | Yes | — | Database name |
| `DB_USERNAME` | Yes | — | Database username |
| `DB_PASSWORD` | Yes | — | Database password |
| `REDIS_HOST` | Yes | `localhost` | Redis host |
| `REDIS_PORT` | No | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | Yes | `localhost:9092` | Kafka brokers |
| `JWT_SECRET` | Yes | — | Secret key for JWT signing (min 256-bit) |
| `JWT_EXPIRATION_MS` | No | `3600000` | Access token TTL (ms) |
| `SMTP_HOST` | No | — | SMTP server for email notifications |
| `SMTP_PORT` | No | `587` | SMTP port |
| `SLACK_WEBHOOK_URL` | No | — | Slack incoming webhook URL |
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active Spring profile |
| `LOG_LEVEL` | No | `INFO` | Root log level |
 
---
 
## 🗺️ Roadmap
 
- [ ] **v1.1** — GraphQL API alongside REST
- [ ] **v1.2** — AI-powered anomaly detection (behavioral analytics)
- [ ] **v1.3** — Policy-as-code (YAML/OPA policy definitions)
- [ ] **v2.0** — Multi-tenancy with tenant isolation
- [ ] **v2.1** — Blockchain-based immutable audit log option
- [ ] **v2.2** — Native integrations: AWS CloudTrail, Azure Monitor, Splunk
- [ ] **v2.3** — Automated remediation workflows
---
 
## 🤝 Contributing
 
Contributions are welcome! Please follow these steps:
 
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m 'feat: add your feature'`
4. Push to the branch: `git push origin feature/your-feature-name`
5. Open a Pull Request
Please follow the [Conventional Commits](https://www.conventionalcommits.org/) specification for commit messages and ensure all tests pass before submitting.
 
**Code Style:** The project uses Google Java Style Guide. Run `mvn spotless:apply` to auto-format before committing.
 
---
 
## 📄 License
 
This project is licensed under the [MIT License](LICENSE).
 
---
 
> Built with ☕ and Spring Boot. Keeping organizations audit-ready, always.
