# ADR-003 — Tenant-aware composite foreign keys

Status: ACCEPTED

Date: 2026-08-31

## Context

Some tenant-owned operational records reference other tenant-owned aggregates. A scalar UUID foreign key confirms that the referenced record exists, but cannot prove that it belongs to the same Tenant as the referencing record. Inbound Shipment must prevent cross-tenant Warehouse and SKU references even for direct database writes.

## Decision

When a tenant-owned record carries `tenant_id`, references another tenant-owned aggregate, and a cross-tenant mismatch is an important database invariant, FulfillOps may use a tenant-aware composite foreign key:

```text
(tenant_id, resource_id) -> (tenant_id, id)
```

Referenced tables declare the required `UNIQUE (tenant_id, id)` target. This decision is selective. It does not require every tenant-descendant table to duplicate `tenant_id`; normalized descendants such as the Warehouse location hierarchy retain their immediate-parent references.

## Consequences

Positive:
- PostgreSQL rejects cross-tenant mismatches through direct writes;
- tenant ownership is explicit on resources that need tenant-aware references.

Trade-offs:
- selected referencing rows intentionally repeat `tenant_id`;
- composite unique indexes add storage and write cost;
- migrations and foreign keys are more verbose.

The convention complements ADR-001: Java models still use scalar IDs and do not add cross-module JPA relationships.
