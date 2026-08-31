# ADR-001 â€” Scalar cross-module aggregate references

Status: ACCEPTED

Date: 2026-08-31

## Context

FulfillOps is a modular monolith. TenantMembership connects the independently owned User and Tenant aggregates, and future Warehouse, Inventory and Fulfillment modules will need similar references. Direct cross-module JPA relationships make repository access and object navigation convenient, but couple persistence models and can create unintended loading behavior.

## Decision

Across module boundaries, Java domain and persistence models use scalar aggregate references such as `UUID tenantId` and `UUID userId`. PostgreSQL foreign keys retain referential integrity. Cross-module JPA entity relationships are avoided by default.

A JPA navigational relationship may be introduced later only when a concrete ownership or navigation requirement justifies the added coupling.

Cross-module workflows verify prerequisites through narrow application methods owned by the referenced module rather than directly importing another module's repository.

## Alternatives considered

### Cross-module `@ManyToOne` relationships

Pros:
- convenient object navigation;
- ORM-managed joins are easy to express.

Cons:
- couples module persistence models;
- risks accidental lazy loading and broader repository reach-through;
- makes boundaries harder to preserve as the monolith grows.

### Scalar IDs without foreign keys

Pros:
- low Java-level coupling.

Cons:
- loses database referential integrity;
- permits orphaned references through direct database writes or application defects.

## Consequences

Positive:
- module ownership stays explicit;
- database integrity remains enforced;
- fetch behavior is predictable;
- future module extraction has fewer persistence-model dependencies.

Negative / trade-offs:
- application services must explicitly obtain data from another module when it is needed;
- joins and reporting queries may need deliberate projections or SQL;
- scalar IDs do not themselves authorize tenant access.

## Follow-up

- Apply the convention to future cross-module references unless a documented exception is justified.
- Add authentication and RBAC in FO-007 without changing global User ownership.
