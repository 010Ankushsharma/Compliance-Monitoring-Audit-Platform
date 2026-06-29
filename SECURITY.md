# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0.x   | ✅ Yes     |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub Issues.**

### How to Report

Email **security@company.com** with:

1. **Description** — what the vulnerability is
2. **Impact** — what an attacker could achieve
3. **Reproduction steps** — how to trigger it
4. **Affected versions** — which version(s) are affected
5. **Suggested fix** (optional)

### Response Timeline

| Step | Timeline |
|---|---|
| Acknowledgement | Within 48 hours |
| Initial assessment | Within 5 business days |
| Fix for critical issues | Within 14 days |
| Public disclosure | After fix is released |

## Security Design

This platform is designed with security at every layer:

- **Authentication**: JWT/HS512, rotating refresh tokens, BCrypt-12 password hashing
- **Authorisation**: 5-level RBAC enforced at both HTTP filter and method level
- **Audit Trail**: SHA-256 hash-chained, immutable at DB + application layer
- **Transport**: TLS required in production (enforced by K8s Ingress)
- **Secrets**: Never logged, never in DTOs, environment-variable driven
- **Rate Limiting**: Per-IP token bucket (Bucket4j)
- **Dependencies**: OWASP dependency-check in every CI run + nightly scan
- **Container**: Non-root user, read-only root filesystem, dropped capabilities
- **Network**: Kubernetes NetworkPolicy with default-deny

## Known Limitations

- TOTP MFA is scaffolded but the full TOTP verification flow is not yet implemented
- Refresh tokens use UUID format — consider adding JTI claim to access tokens for revocation
- Report files are stored on local filesystem — migrate to S3/GCS for production scale
