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

All REST API errors use this shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed.",
  "status": 400,
  "path": "/api/v1/warehouses",
  "timestamp": "2026-01-01T12:00:00Z",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "fieldErrors": [
    {
      "field": "name",
      "message": "must not be blank"
    }
  ]
}
```

Fields:
- `code` â€” stable machine-readable error code.
- `message` â€” safe client-facing summary.
- `status` â€” numeric HTTP status.
- `path` â€” request URI path.
- `timestamp` â€” UTC ISO-8601 instant.
- `requestId` â€” server-generated UUID for the request.
- `fieldErrors` â€” field-level validation details, or an empty array when not applicable. Entries contain only `field` and `message`; rejected values are never exposed.

Initial error codes:
- `VALIDATION_ERROR` â€” a request body failed Bean Validation.
- `MALFORMED_JSON` â€” a request body could not be parsed.
- `TENANT_NOT_FOUND` â€” a requested Tenant does not exist.
- `TENANT_CODE_CONFLICT` â€” a Tenant code is already in use.
- `USER_NOT_FOUND` â€” a requested User does not exist.
- `USER_EMAIL_CONFLICT` â€” a User email is already in use.
- `TENANT_MEMBERSHIP_CONFLICT` â€” the User already belongs to the Tenant.
- `MEMBERSHIP_NOT_FOUND` â€” a requested TenantMembership does not exist in the URL Tenant scope.
- `WAREHOUSE_NOT_FOUND` â€” a requested Warehouse does not exist in the URL Tenant scope.
- `WAREHOUSE_CODE_CONFLICT` â€” a Warehouse code is already in use in the Tenant.

- `ZONE_NOT_FOUND` / `ZONE_CODE_CONFLICT` — a Zone is absent from its Warehouse scope, or its code is already in use there.
- `AISLE_NOT_FOUND` / `AISLE_CODE_CONFLICT` — an Aisle is absent from its Zone scope, or its code is already in use there.
- `RACK_NOT_FOUND` / `RACK_CODE_CONFLICT` — a Rack is absent from its Aisle/Warehouse scope, or its code is already in use there.
- `BIN_NOT_FOUND` / `BIN_CODE_CONFLICT` — a Bin is absent from its Rack/Warehouse scope, or its code is already in use there.

Every response includes a server-generated `X-Request-Id` header. For error responses, it exactly matches the `requestId` in the JSON body. Client-provided request IDs are not accepted in this phase.

## 6. Validation

Invalid client input returns predictable 4xx responses. Business conflicts should not become generic 500 errors.

FO-003 handles request-body validation. Support for method-level query/path parameter validation failures (`HandlerMethodValidationException`) will be added when an API requires it.

## 7. Idempotency

Retry-prone external create APIs will later support `Idempotency-Key`.

## 8. Tenant identity

Do not treat request-supplied `tenantId` as proof of authorization.

Tenant-scoped routes must include their tenant scope in persistence queries. Until authentication is implemented, this is resource scoping rather than authorization. The initial membership routes are:

```text
POST /api/v1/users
GET  /api/v1/users/{id}
POST /api/v1/tenants/{tenantId}/memberships
GET  /api/v1/tenants/{tenantId}/memberships/{membershipId}
PATCH /api/v1/tenants/{tenantId}/memberships/{membershipId}/role
POST /api/v1/tenants/{tenantId}/warehouses
GET  /api/v1/tenants/{tenantId}/warehouses/{warehouseId}
POST /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/zones
GET  /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/zones/{zoneId}
POST /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/zones/{zoneId}/aisles
GET  /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/zones/{zoneId}/aisles/{aisleId}
POST /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/aisles/{aisleId}/racks
GET  /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/aisles/{aisleId}/racks/{rackId}
POST /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/racks/{rackId}/bins
GET  /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/racks/{rackId}/bins/{binId}
```

Location routes keep Tenant and Warehouse explicit, then include only the immediate parent necessary for the resource. Services verify the full ownership chain before use, so a known ID cannot be read or created under another Warehouse or parent path. These routes are structural scoping only; authentication and HTTP authorization remain deferred.

The role PATCH request is `{"role":"ADMIN"}` or `{"role":"VIEWER"}`. It is tenant-scoped but is not authenticated or authorized in FO-007. Unknown role enum JSON uses the existing `MALFORMED_JSON` error and a missing/null role uses `VALIDATION_ERROR`.

## 9. Documentation

Public APIs should use OpenAPI/Swagger once controllers exist.
