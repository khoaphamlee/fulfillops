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
- `SKU_NOT_FOUND` — a requested SKU does not exist in the URL Tenant scope.
- `SKU_CODE_CONFLICT` — a canonical SKU code is already in use in the Tenant.
- `INBOUND_SHIPMENT_NOT_FOUND` — a requested inbound shipment does not exist in the URL Tenant/Warehouse scope.
- `INBOUND_SHIPMENT_DUPLICATE_SKU_LINE` — a SKU is repeated in one inbound shipment request.
- `RECEIVING_RECEIPT_NOT_FOUND` — a requested receipt does not exist in the URL Shipment scope.
- `RECEIVING_PLANNED_LINE_NOT_FOUND` — a requested planned inbound line does not belong to the scoped Shipment.
- `RECEIVING_DUPLICATE_LINE` — a planned line is repeated in one receipt.
- `RECEIVING_QUANTITY_EXCEEDS_EXPECTED` — receipt quantity would exceed the planned line's remaining expected quantity.

Every response includes a server-generated `X-Request-Id` header. For error responses, it exactly matches the `requestId` in the JSON body. Client-provided request IDs are not accepted in this phase.

## 6. Validation

Invalid client input returns predictable 4xx responses. Business conflicts should not become generic 500 errors.

FO-003 handles request-body validation. Support for method-level query/path parameter validation failures (`HandlerMethodValidationException`) will be added when an API requires it.

## 7. Idempotency

Receiving Receipt POST requires the case-sensitive `Idempotency-Key` header. A key is 1-128 characters matching `[A-Za-z0-9._:-]+`; it is never trimmed or canonicalized. Missing keys return `RECEIVING_IDEMPOTENCY_KEY_REQUIRED` (400), invalid keys return `RECEIVING_IDEMPOTENCY_KEY_INVALID` (400), and the policy is Receiving-specific rather than a platform-wide idempotency contract.

The key scope is Tenant plus InboundShipment. The first successful command returns `201 Created` and a Receipt Location. A retry with the same key and same semantic request returns `200 OK`, the same Receipt Location, and the same Receipt resource without creating another physical receipt. A reused key with a different semantic request returns `RECEIVING_IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_REQUEST` (409). Replay represents the persisted Receipt using current derived receiving quantities; it is not a byte-for-byte historical HTTP-response snapshot.

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
POST /api/v1/tenants/{tenantId}/skus
GET  /api/v1/tenants/{tenantId}/skus/{skuId}
POST /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments
GET  /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}
POST /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}/receipts
GET  /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}/receipts/{receiptId}
GET  /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inbound-shipments/{shipmentId}/receiving-progress
GET  /api/v1/tenants/{tenantId}/warehouses/{warehouseId}/inventory/skus/{skuId}
```

Location routes keep Tenant and Warehouse explicit, then include only the immediate parent necessary for the resource. Services verify the full ownership chain before use, so a known ID cannot be read or created under another Warehouse or parent path. These routes are structural scoping only; authentication and HTTP authorization remain deferred.

SKU routes are tenant-scoped but Warehouse-independent. Valid SKU codes are returned in canonical uppercase form; case variants identify the same SKU within a Tenant.

Inbound shipment creation is atomic and represents expected, discrete whole-unit quantities only. It has no status transition or receiving behavior; creating it does not create Inventory.

Receiving receipt creation is append-only and supports partial whole-unit receipt. It rejects over-receipt through the scoped Shipment lock and cumulative receipt-history check. Receipt POST requires the Receiving-specific Idempotency-Key behavior above; Receiving progress is derived from planned and receipt lines, with no persisted receiving status/counter.

Inventory GET returns Warehouse-level current on-hand quantity for one scoped SKU. If the Tenant, Warehouse, and SKU exist but no balance row exists, it returns `200` with `onHandQuantity: 0` and null `createdAt`/`updatedAt`; GET never creates a balance row. Inventory has no public write endpoint in this phase.

The role PATCH request is `{"role":"ADMIN"}` or `{"role":"VIEWER"}`. It is tenant-scoped but is not authenticated or authorized in FO-007. Unknown role enum JSON uses the existing `MALFORMED_JSON` error and a missing/null role uses `VALIDATION_ERROR`.

## 9. Documentation

Public APIs should use OpenAPI/Swagger once controllers exist.
