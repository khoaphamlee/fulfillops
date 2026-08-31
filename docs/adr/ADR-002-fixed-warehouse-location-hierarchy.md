# ADR-002 — Fixed warehouse location hierarchy

Status: ACCEPTED

Date: 2026-08-31

## Context

FulfillOps needs physical warehouse locations for later inventory, putaway, picking, and cycle-count work. The currently defined hierarchy is fixed: Warehouse -> Zone -> Aisle -> Rack -> Bin. A generic arbitrary-depth location table would be flexible, but it would require application rules or database triggers to ensure valid parent types and depth.

## Decision

Use four typed tables: `warehouse_zones`, `warehouse_aisles`, `warehouse_racks`, and `warehouse_bins`. Each row stores a scalar UUID reference only to its immediate parent, with a PostgreSQL foreign key. No lower-level row duplicates `tenant_id` or `warehouse_id`; tenant ownership is derived through the normalized parent chain.

Codes are unique within the immediate parent, and a full locator remains derived from hierarchy data rather than stored as a denormalized path. Java entities use scalar IDs and avoid JPA navigational object graphs.

## Alternatives considered

### Generic `warehouse_locations` tree

This supports arbitrary depth and new location kinds with fewer tables. It also makes typed parent-child validity, fixed-depth constraints, and Inventory's expected Bin reference harder to enforce without additional database logic or application checks.

### Redundant tenant and warehouse ownership columns at every level

This can simplify some tenant-filtered queries, but duplicates state and requires composite constraints or triggers to prevent inconsistent ancestry. The fixed hierarchy does not justify that complexity yet.

## Consequences

The schema directly enforces each parent relationship and keeps future Bin references simple. The trade-off is a migration and endpoint for each fixed level, and a future arbitrary-depth hierarchy would require a deliberate migration rather than being accommodated automatically. This is not a universal preference over generic trees; it fits the current WMS hierarchy and integrity requirements.
