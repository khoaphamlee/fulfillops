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

## 6. Task report

Report:
- test command,
- pass/fail result,
- scenarios covered,
- known gaps.
