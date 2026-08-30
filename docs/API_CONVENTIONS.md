# FulfillOps — API Conventions

## 1. Base path

```text
/api/v1
```

## 2. Resource naming

Use plural nouns:

```text
/api/v1/warehouses
/api/v1/skus
/api/v1/fulfillment-requests
/api/v1/shipments
```

## 3. HTTP semantics

- GET — read
- POST — create/command
- PUT — full replacement where appropriate
- PATCH — partial update/state transition
- DELETE — delete/deactivate where allowed

## 4. Pagination

Likely convention:

```text
?page=0&size=20&sort=createdAt,desc
```

## 5. Error contract

Target shape:

```json
{
  "code": "INVENTORY_INSUFFICIENT",
  "message": "Insufficient available inventory.",
  "status": 409,
  "path": "/api/v1/fulfillment-requests",
  "timestamp": "2026-01-01T12:00:00Z",
  "requestId": "..."
}
```

Exact fields will be finalized in FO-003.

## 6. Validation

Invalid client input returns predictable 4xx responses. Business conflicts should not become generic 500 errors.

## 7. Idempotency

Retry-prone external create APIs will later support `Idempotency-Key`.

## 8. Tenant identity

Do not treat request-supplied `tenantId` as proof of authorization.

## 9. Documentation

Public APIs should use OpenAPI/Swagger once controllers exist.
