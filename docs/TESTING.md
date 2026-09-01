# FulfillOps — Testing Strategy

## 1. Goal

Tests prove business correctness, security boundaries and integration behavior.

## 2. Layers

### Unit
For isolated business rules and algorithms.

### Integration
For repositories, transactions, APIs, tenant isolation and database constraints.

### End-to-end
Use selectively for critical cross-module workflows.

## 3. Core invariants

Later tests must prove:
- tenant cannot read another tenant's data,
- SKU/warehouse codes respect tenant-scoped uniqueness,
- inventory cannot become invalid,
- reservation cannot exceed available stock,
- duplicate external requests do not create duplicate fulfillment,
- picking/packing/shipment transitions are valid.

## 4. Concurrency test

```text
Given available stock = 1
When two concurrent requests reserve quantity = 1
Then exactly one reservation succeeds.
```

## 5. Testcontainers

Use Testcontainers PostgreSQL when persistence integration tests begin.

Add Redis/Kafka containers only in their roadmap phases.

PostgreSQL integration tests use two singleton containers per Maven test JVM:

- a shared current-schema application server for Spring/API tests and latest-schema verification;
- a separate migration server for historical migration-evolution tests.

The shared application database is cleaned before each test with a metadata-driven `TRUNCATE ... RESTART IDENTITY CASCADE` over ordinary `fulfillops` tables. Flyway schema history is preserved. This is required because RANDOM_PORT HTTP requests commit outside a test-thread transaction, so blanket test rollback is not reliable isolation.

Migration-evolution scenarios recreate their dedicated database before each scenario, then control Flyway target versions explicitly. This ensures an independent scenario starts empty even though its PostgreSQL server is shared.

Suite-level JUnit parallel execution is disabled while tests share the mutable application database. This does not prevent real in-method business-concurrency tests from using independent transactions and worker threads. No-database tests do not opt into either container and remain Docker-independent.

Static containers are shared once per Surefire JVM. If Maven test forks are enabled later, each fork starts its own pair of containers.

Receiving idempotency tests use the shared application server to prove persisted same-key replay, mismatch conflicts, Tenant/Shipment key scope, and real concurrent same-key receipt posting. Migration evolution tests use the separate migration server to prove that pre-idempotency Receipt rows migrate with null metadata.

## 6. Task report

Report:
- test command,
- pass/fail result,
- scenarios covered,
- known gaps.
