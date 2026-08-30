# FulfillOps — Database Rules

## 1. Primary database

PostgreSQL is the initial system of record.

Schema migrations will use Flyway once the backend is bootstrapped.

## 2. Tenant-scoped data

Tenant-owned tables should carry a tenant identifier unless an ADR establishes another strategy.

Likely future constraints:

```text
UNIQUE (tenant_id, warehouse_code)
UNIQUE (tenant_id, sku_code)
```

A globally unique technical ID does not remove the need for tenant authorization.

## 3. Early entity candidates

- users
- tenants
- tenant_memberships
- warehouses
- warehouse_locations
- skus
- inbound_shipments
- inbound_lines
- inventory_balances
- inventory_movements
- fulfillment_requests
- fulfillment_lines
- inventory_reservations
- picking_tasks
- packing_units
- shipments
- shipment_events
- drivers
- vehicles
- proof_of_delivery
- audit_logs

## 4. Inventory modeling

Target concepts:

```text
InventoryBalance
- tenant
- warehouse
- location
- sku
- on_hand
- reserved
- unavailable
- version

InventoryMovement
- tenant
- sku
- source_location
- destination_location
- movement_type
- quantity
- reference_type
- reference_id
- created_at
```

## 5. Concurrency

Inventory reservation will require explicit concurrency control.

Candidate approaches to evaluate:
- optimistic locking,
- pessimistic row locking,
- conditional SQL updates,
- distributed locking only if later architecture truly requires it.

## 6. Indexing

Every index must answer a query/use case.

Expected dimensions include tenant, warehouse, SKU, status, external reference, shipment number and timestamps.

## 7. Migrations

- no manual production schema drift,
- no `ddl-auto=update` as migration strategy,
- every schema change gets a Flyway migration,
- destructive migrations require explicit review.
