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

Every response includes a server-generated `X-Request-Id` header. For error responses, it exactly matches the `requestId` in the JSON body. Client-provided request IDs are not accepted in this phase.

## 6. Validation

Invalid client input returns predictable 4xx responses. Business conflicts should not become generic 500 errors.

FO-003 handles request-body validation. Support for method-level query/path parameter validation failures (`HandlerMethodValidationException`) will be added when an API requires it.

## 7. Idempotency

Retry-prone external create APIs will later support `Idempotency-Key`.

## 8. Tenant identity

Do not treat request-supplied `tenantId` as proof of authorization.

## 9. Documentation

Public APIs should use OpenAPI/Swagger once controllers exist.
