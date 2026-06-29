# Contributing to Compliance Monitoring & Audit Platform

Thank you for your interest in contributing! This document explains how to get started.

## 🚀 Getting Started

```bash
# 1. Fork the repository on GitHub
# 2. Clone your fork
git clone https://github.com/<your-username>/Compliance-Monitoring-Audit-Platform.git
cd Compliance-Monitoring-Audit-Platform

# 3. Add upstream remote
git remote add upstream https://github.com/010Ankushsharma/Compliance-Monitoring-Audit-Platform.git

# 4. Start infrastructure
docker-compose up -d postgres redis kafka

# 5. Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## 🌿 Branch Strategy

```
main        ← production (protected — PR + approval required)
develop     ← integration (protected — PR required)
feature/*   ← new features (branch from develop)
fix/*       ← bug fixes
security/*  ← security patches (expedited review)
```

## ✅ Pull Request Checklist

Before submitting a PR, make sure:

- [ ] Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/)
- [ ] Code passes `./mvnw spotless:check` (Google Java Format)
- [ ] New service methods have unit tests
- [ ] New endpoints have integration tests
- [ ] Sensitive fields (passwords, secrets, tokens) excluded from all DTOs
- [ ] `@Auditable` added to all state-changing service methods
- [ ] Cache evicted on all write operations (`@CacheEvict`)
- [ ] Multi-tenant isolation verified (`principal.canManage(orgId)`)
- [ ] PR description explains the **why**, not just the what

## 📝 Commit Message Format

```
type(scope): short description

Body (optional — explain why, not what)

Refs: #issue-number
```

**Types:** `feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci` `chore` `revert` `security`

**Scopes:** `auth` `policy` `audit` `violation` `alert` `report` `dashboard` `kafka` `security` `config` `docker` `k8s` `ci` `deps`

**Examples:**
```
feat(policy): add cron validation for evaluation schedule
fix(auth): clear lockout counter on successful login
security(audit): enforce SHA-256 hash chain verification
test(violation): add unit tests for status transition guard
```

## 🐛 Reporting Bugs

Open a GitHub Issue with:
- Steps to reproduce
- Expected vs actual behaviour
- Java / Spring Boot version
- Relevant logs (sanitise any secrets)

## 💡 Suggesting Features

Open a GitHub Discussion under the **Ideas** category before opening a PR for large features.

## 🔒 Security Vulnerabilities

**Do not open a public issue for security vulnerabilities.**
Email directly: `security@company.com`
Include: description, reproduction steps, and potential impact.

## 📄 License

By contributing, you agree that your contributions will be licensed under the MIT License.
