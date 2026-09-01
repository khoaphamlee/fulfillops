# FulfillOps — Task Board

Task IDs are stable references for prompts, commits and documentation.

## Phase 0

### FO-001 — Repository and project-brain setup
Status: DONE

Scope:
- create repository,
- create `AGENTS.md`,
- create core `/docs`,
- create ADR template,
- establish task naming,
- commit foundation.

Done when:
- documents exist,
- Codex can explain the project constraints from the files,
- no application code has been introduced accidentally,
- first Git commit is created.

## Phase 1 backlog

### FO-002 — Bootstrap Spring Boot backend
Status: DONE

### FO-003 — API error contract and validation foundation
Status: DONE
    
### FO-004 — Local PostgreSQL + Flyway
Status: DONE

### FO-005 — Tenant aggregate
Status: DONE

### FO-006 — User and tenant membership
Status: DONE

### FO-007 — Tenant RBAC foundation
Status: DONE

### FO-008 — Warehouse
Status: DONE

### FO-009 — Warehouse location hierarchy
Status: DONE

### FO-010 — SKU / item master
Status: DONE

### FO-011 — Inbound shipment
Status: DONE

### FO-012 — Receiving workflow
Status: DONE

### FO-012.5 — Receiving idempotency
Status: DONE

Scope:
- require a Receiving-specific persisted Idempotency-Key for Receipt POST,
- replay the original Receipt for a same-key semantic retry,
- reject same-key different-command retries,
- preserve Receiving concurrency and migration correctness before Inventory.

### FO-013 — Inventory balance foundation
Status: DONE

Scope:
- Warehouse-level current on-hand balance per Tenant/Warehouse/SKU,
- synchronous Receiving-to-Inventory update with PostgreSQL atomic upsert,
- Inventory read API and cross-Shipment concurrency coverage.

### FO-014 — Inventory ledger / movements
Status: TODO

### FO-015 — Putaway task
Status: TODO

### FO-016 — Fulfillment request
Status: TODO

### FO-017 — Allocation strategy v1
Status: TODO

### FO-018 — Inventory reservation
Status: TODO

### FO-019 — Reservation concurrency tests
Status: TODO

### FO-020 — Picking, packing and shipment creation
Status: TODO
