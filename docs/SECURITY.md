# FulfillOps — Security

## 1. Goals

- authenticate users,
- authorize actions by role,
- enforce tenant isolation,
- protect secrets,
- secure external integrations,
- audit operational changes.

## 2. Planned authentication evolution

Early:
- email/password,
- password hashing,
- access token / refresh token.

Later:
- OAuth2,
- OpenID Connect,
- optional Keycloak/IdP integration.

## 3. Tenant authorization

```text
user -> tenant_membership -> role -> allowed operation
```

A valid user is not automatically authorized for every tenant.

Roles belong to `TenantMembership`, never to the global User. A User can therefore hold a different role in each Tenant. FO-007 establishes one fixed role per membership: `ADMIN` for future membership-administration capability and `VIEWER` as the least-privilege role. Permissions remain deferred until implemented operational actions require them.

FO-007 does not authenticate a User or enforce HTTP authorization. A later Security/onboarding phase must resolve the authenticated User and active Tenant membership before applying this model, and must define how a Tenant receives its initial ADMIN membership. Existing memberships are not promoted automatically.

## 4. Conceptual tenant roles

- OWNER
- ADMIN
- WAREHOUSE_MANAGER
- WAREHOUSE_STAFF
- DISPATCHER
- DRIVER
- VIEWER

FO-007 intentionally implements only ADMIN and VIEWER. The remaining conceptual roles require concrete operational responsibilities before they are introduced.

## 5. Rules

- Never store plaintext passwords.
- Never log password/token/secret values.
- Do not trust client-provided tenant identity.
- Rate-limit security-sensitive and integration endpoints when Redis is introduced.
- Use HTTPS when deployed.
- Keep secrets out of source control.
- Protect admin operations with authorization tests.

Until authenticated administrative workflows exist, concurrent membership role updates are last-commit-wins. This behavior must be reconsidered when role changes become protected and operationally consequential.

## 6. Cross-tenant test rule

For every tenant-scoped resource:

```text
Tenant A creates resource R.
Tenant B attempts to access R.
Access is denied / resource is not exposed.
```

## 7. Integration security

Later integrations may use:
- API keys,
- HMAC signatures,
- timestamp validation,
- replay protection,
- idempotency keys,
- secret rotation.
