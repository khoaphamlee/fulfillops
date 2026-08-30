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

## 4. Conceptual tenant roles

- OWNER
- ADMIN
- WAREHOUSE_MANAGER
- WAREHOUSE_STAFF
- DISPATCHER
- DRIVER
- VIEWER

## 5. Rules

- Never store plaintext passwords.
- Never log password/token/secret values.
- Do not trust client-provided tenant identity.
- Rate-limit security-sensitive and integration endpoints when Redis is introduced.
- Use HTTPS when deployed.
- Keep secrets out of source control.
- Protect admin operations with authorization tests.

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
