# FulfillOps — Architecture

## 1. Initial architecture

FulfillOps starts as a **modular monolith**.

Why:
- simpler development/debugging,
- easier local transactions,
- lower operational complexity,
- clear domain boundaries before service extraction.

## 2. Planned logical modules

```text
identity
tenant
membership
warehouse
catalog
inbound
inventory
fulfillment
picking
packing
shipping
transportation
notification
audit
integration
analytics
subscription
common
```

Not all modules must exist from day one.

## 3. Dependency principle

A module owns its business rules and persistence model.

Avoid allowing every module to manipulate every other module's repositories directly. Prefer explicit application services/use cases for cross-module workflows.

## 4. Layering guideline

A practical structure may evolve toward:

```text
<module>/
  domain/
  application/
  infrastructure/
  presentation/
```

Do not create ceremony solely for folder count.

## 5. API boundary

Controllers should parse/validate HTTP input, call application services and map results. Controllers should not own inventory, allocation or authorization business rules.

## 6. Persistence boundary

PostgreSQL is the initial system of record. Spring Data JPA/Hibernate is the default ORM layer, with JPQL/native SQL allowed when justified.

## 7. Transactions

Early phases use local PostgreSQL transactions.

Later phases may introduce:
- transactional outbox,
- eventual consistency,
- idempotent consumers,
- Saga-style workflows.

## 8. Future direction

Service extraction is a later learning phase only after modular boundaries are proven.

## 9. Architecture decision rule

Create/update an ADR for decisions affecting:
- module boundaries,
- persistence strategy,
- tenancy,
- consistency,
- messaging,
- security model,
- deployment strategy.
