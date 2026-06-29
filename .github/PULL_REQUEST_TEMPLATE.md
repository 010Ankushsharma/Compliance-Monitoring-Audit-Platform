## Description
<!-- What does this PR do? Link to the issue it closes. -->

Closes #

## Type of Change
- [ ] 🐛 Bug fix
- [ ] ✨ New feature
- [ ] 🔒 Security fix
- [ ] 📖 Documentation
- [ ] ♻️ Refactor
- [ ] 🧪 Tests
- [ ] 🔧 CI/CD / Infrastructure

## Checklist
- [ ] Commit messages follow Conventional Commits (`type(scope): subject`)
- [ ] `./mvnw spotless:check` passes (Google Java Format)
- [ ] Unit tests added / updated
- [ ] `@Auditable` added to state-changing service methods
- [ ] Cache evicted on write operations (`@CacheEvict`)
- [ ] Multi-tenant isolation verified (`principal.canManage(orgId)`)
- [ ] No secrets, passwords, or tokens in DTOs, logs, or responses
- [ ] Swagger `@Operation` annotation added to new endpoints

## Testing
<!-- Describe how you tested this change -->

## Screenshots (if UI-related)
