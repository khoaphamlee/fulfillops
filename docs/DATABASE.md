# FulfillOps — Database Rules

## 1. Primary database

PostgreSQL is the initial system of record.

Schema migrations will use Flyway once the backend is bootstrapped.

## 2. Tenant-scoped data

Tenant-owned tables should carry a tenant identifier unless an ADR establishes another strategy.

The `fulfillops.tenants` table is the tenancy root, so it is the deliberate exception: it does not contain `tenant_id`. Tenant codes are globally unique and immutable. Future tenant-owned operational tables must carry tenant ownership.

`fulfillops.users` is also global and must not contain `tenant_id`. `fulfillops.tenant_memberships` connects a User to a Tenant and is tenant-scoped: all membership resource queries include the tenant scope. Membership tables store scalar `tenant_id` and `user_id` aggregate references in Java while named PostgreSQL foreign keys retain referential integrity.

Tenant memberships have exactly one fixed role stored as text: `ADMIN` or `VIEWER`. New and pre-existing memberships use `VIEWER` by default/backfill to preserve least privilege. The `chk_tenant_memberships_role` constraint rejects unknown persisted role values. Membership `updated_at` records real role changes; a same-role request is a no-op. Permissions and custom roles remain deferred.

`fulfillops.warehouses` is the first tenant-owned operational aggregate. It carries immutable scalar `tenant_id` ownership with a named foreign key to the tenancy root. Warehouse code is an immutable lowercase kebab-case identifier unique within its Tenant through `UNIQUE (tenant_id, code)`; it is not globally unique.

Warehouse physical locations use a fixed normalized hierarchy: Warehouse -> Zone -> Aisle -> Rack -> Bin. `warehouse_zones` references Warehouse, then each lower table references only its immediate parent (`zone_id`, `aisle_id`, or `rack_id`). Lower levels deliberately do not duplicate `tenant_id` or `warehouse_id`; PostgreSQL foreign keys enforce each link and application scoping verifies the ownership chain. Codes are lowercase kebab-case and unique within their immediate parent scope. Bin is the current physical leaf, but FO-009 does not establish inventory or a rule that only Bins may hold stock. See ADR-002.

`fulfillops.skus` holds tenant-owned item identities independently of Warehouses and Bins. SKU codes are unique within a Tenant and canonicalized to uppercase using `Locale.ROOT`, so `abc-001` and `ABC-001` identify the same SKU there. The database uniquely constrains `(tenant_id, code)` and rejects noncanonical or malformed persisted codes. SKU remains master data only: Inventory will later connect it to warehouse/bin stock and ledger state.

`fulfillops.inbound_shipments` records atomically-created expected goods for one Tenant and Warehouse. Its aggregate-owned lines identify one SKU per shipment and a strictly positive whole-unit expected quantity. Inbound planning does not record physical receipt or create Inventory. Where an important reference must prove that two tenant-owned records share the same Tenant, tenant-aware composite foreign keys use `(tenant_id, id)` targets; the intentional repeated `tenant_id` and composite indexes trade small write/storage cost for stronger direct-write integrity. This does not apply automatically to normalized descendants such as the physical-location hierarchy. See ADR-003.

Receiving uses append-only `receiving_receipts` and `receiving_receipt_lines`; planned expected quantities remain unchanged. Receipt history is the source for cumulative received and remaining quantities, and partial receipts are allowed. Receipt posting takes a PostgreSQL pessimistic lock on the scoped InboundShipment root, sums prior receipt history, and rejects an over-receipt. The cumulative invariant is workflow-enforced by that lock and transaction, not by a standalone SQL CHECK; direct SQL can bypass it. Receiving currently supports whole units only, does not mutate Inventory, and does not choose a Bin/putaway destination. Receipts are immutable history; future corrections should use explicit compensating records.

Receiving Receipt POST is retry-safe through persisted Receipt metadata. `receiving_receipts` stores an immutable client idempotency key and SHA-256 semantic request fingerprint, with `UNIQUE (tenant_id, inbound_shipment_id, idempotency_key)`. Receipt posting takes the scoped Shipment lock before resolving the key, so concurrent same-key requests serialize and replay the one committed Receipt. The fingerprint canonicalizes validated, distinct planned-line UUID/quantity pairs without line-order significance. Failed commands do not consume a key because metadata and Receipt history commit atomically. Historical pre-V11 Receipts retain null metadata and are not assigned fabricated client keys. This is Receiving-specific, not a generic platform idempotency facility.

Inventory uses `inventory_balances` as authoritative mutable Warehouse-level current state: one `on_hand_quantity` cell per Tenant, Warehouse, and SKU. It is whole-unit on-hand only, with no Bin, location, available, reserved, or damaged quantity. Composite Tenant/Warehouse and Tenant/SKU foreign keys reject cross-tenant direct writes. Receiving applies positive SKU increments through PostgreSQL atomic UPSERTs, ordered by SKU UUID, in the same transaction as a new Receipt and ReceiptLines. An idempotent replay does not update the balance or timestamps.

`inventory_ledger_entries` is immutable movement history. Each newly committed ReceivingReceiptLine creates exactly one `RECEIVING` entry with a positive signed `quantity_delta`, scalar ReceiptLine provenance, and a Tenant-aware ReceiptLine FK. Ledger append is flushed before the Balance UPSERT, but both remain in the same transaction and therefore commit or roll back together. Ledger Warehouse/SKU and ReceiptLine are each Tenant-aware FK-protected; deriving that Warehouse/SKU from the ReceiptLine's Receiving source chain remains an application invariant. No historical Ledger rows are fabricated for pre-V13 Balances: Ledger completeness starts with V13-supported movements, so only fresh V13+ Inventory state can reconcile Ledger deltas directly to Balance without a future explicit baseline. Receiving progress remains Shipment expected-versus-received history, not Inventory state.

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
- warehouse_zones
- warehouse_aisles
- warehouse_racks
- warehouse_bins
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

A composite unique constraint also provides an index in PostgreSQL. `UNIQUE (tenant_id, user_id)` on tenant memberships supports the current tenant-prefixed membership access pattern; add additional indexes only when a concrete query requires them.

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
