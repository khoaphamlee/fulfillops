# FulfillOps — Database Rules

## 1. Primary database

PostgreSQL is the initial system of record.

Schema migrations will use Flyway once the backend is bootstrapped.

## 2. Tenant-scoped data

Tenant-owned tables should carry a tenant identifier unless an ADR establishes another strategy.

The `fulfillops.tenants` table is the tenancy root, so it is the deliberate exception: it does not contain `tenant_id`. Tenant codes are globally unique and immutable. Future tenant-owned operational tables must carry tenant ownership.

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

## 8. Application schema namespace

FulfillOps application objects belong in the `fulfillops` PostgreSQL schema. Flyway's schema-history table may remain in the default PostgreSQL schema while application objects are created as schema-qualified names, for example `fulfillops.tenants`.

Do not change this convention without a deliberate schema configuration decision.

## 9. Flyway migration workflow

Migration files live in `backend/src/main/resources/db/migration` and use this naming convention:

```text
V<positive_integer>__<lowercase_underscore_description>.sql
```

For example:

```text
V1__create_fulfillops_schema.sql
V2__create_tenant_table.sql
```

For local development:

1. Start PostgreSQL with `docker compose up -d postgres`.
2. Run the application or integration tests.
3. Flyway validates applied migrations and applies pending migrations before application use.
4. Add a new versioned migration for every schema change.

Applied migrations are immutable: never edit, rename, or delete a migration that may already have been applied. Never manually edit a shared schema outside Flyway. Flyway clean remains disabled in application configuration, Hibernate automatic schema mutation is prohibited, and destructive migrations require explicit review.
