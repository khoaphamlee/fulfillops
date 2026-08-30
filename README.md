# FulfillOps

**FulfillOps** is a multi-tenant fulfillment and logistics orchestration platform for warehouse and transportation operations.

The project begins as a modular monolith and evolves gradually toward a production-oriented distributed system.

## Core business flow

```text
Inbound
  -> Receiving
  -> Putaway
  -> Inventory Ledger
  -> Fulfillment Request
  -> Allocation
  -> Reservation
  -> Picking
  -> Packing
  -> Shipment
  -> Dispatch
  -> Tracking
  -> Proof of Delivery
```

## Why this project exists

This repository is both:
1. a portfolio-grade Java Backend project, and
2. a backend engineering laboratory.

The project is intentionally phased so Redis, Kafka, Outbox, observability, cloud infrastructure and microservices are introduced only after there is a business problem that justifies them.

## Project documents

Start here:
- `AGENTS.md`
- `docs/REQUIREMENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/ROADMAP.md`
- `docs/TASKS.md`

## Current status

Current phase: **Phase 0 — Project Foundation**

Current task: **FO-001 — Repository and project-brain setup**

No production code exists yet.
