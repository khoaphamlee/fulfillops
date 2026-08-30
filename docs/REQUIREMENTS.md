# FulfillOps — Requirements

## 1. Product statement

FulfillOps is a multi-tenant SaaS platform that coordinates warehouse fulfillment and transportation workflows for logistics companies, 3PL providers, distributors and businesses operating their own warehouses.

The platform is not an e-commerce storefront. It receives fulfillment demand from users or external systems and coordinates physical inventory through warehouse execution and delivery.

## 2. Primary actors

- Platform Admin
- Tenant Owner
- Tenant Admin
- Warehouse Manager
- Warehouse Staff
- Dispatcher
- Driver
- Viewer

## 3. Core domains

- Identity
- Tenant
- Membership / RBAC
- Warehouse
- Warehouse Location
- SKU / Item Master
- Inbound
- Inventory
- Inventory Ledger
- Putaway
- Transfer
- Cycle Count
- Fulfillment
- Allocation
- Reservation
- Picking
- Packing
- Shipment
- Transportation
- Driver
- Vehicle
- Tracking
- Proof of Delivery
- Return / Reverse Logistics
- Notification
- Audit
- Integration / Webhook
- Analytics
- Subscription / Quota

## 4. Core warehouse hierarchy

```text
Warehouse
  -> Zone
      -> Aisle
          -> Rack
              -> Bin
```

## 5. Inventory concepts

The system must distinguish:
- **on_hand** — physically recorded stock.
- **reserved** — stock committed to active fulfillment.
- **available** — stock that can still be allocated.
- **damaged/unavailable** — stock unavailable for normal fulfillment.

Inventory-changing actions must be auditable through inventory movements/ledger entries.

## 6. Inbound flow

```text
Inbound Created
 -> Arrived
 -> Receiving
 -> Quality Check
 -> Accepted / Damaged
 -> Putaway Tasks
 -> Putaway Completed
 -> Inventory Available
```

## 7. Fulfillment flow

```text
Fulfillment Request
 -> Validate
 -> Select/Allocate Warehouse
 -> Reserve Inventory
 -> Generate Picking Tasks
 -> Picking Completed
 -> Packing
 -> Shipment Created
```

The same external request must not create duplicate fulfillment when the integration retries.

## 8. Reservation requirements

Reservation must eventually handle:
- expiration,
- release,
- insufficient stock,
- competing concurrent requests,
- idempotency,
- inventory consistency.

The system must never successfully reserve more stock than is available.

## 9. Transportation flow

```text
Shipment Ready
 -> Driver/Vehicle Assignment
 -> Dispatched
 -> In Transit
 -> Out for Delivery
 -> Delivered
```

Exceptional outcomes include failed delivery, return to warehouse, damaged shipment, wrong address and recipient unavailable.

## 10. Proof of Delivery

A delivery may capture recipient, timestamp, GPS position, signature, photo and notes.

Binary files will later be stored in object storage rather than PostgreSQL.

## 11. Multi-tenancy

Every tenant-scoped operational entity must belong to exactly one tenant unless a documented exception exists.

Tenant A must never be able to read, update or delete Tenant B operational data.

The active tenant must be resolved from trusted authenticated membership context.

## 12. External integrations

Later phases will support:
- fulfillment request API,
- webhook callbacks,
- signed webhook delivery,
- retry,
- delivery logs,
- replay,
- idempotency keys.

## 13. Non-functional requirements

The mature system should demonstrate:
- correctness under concurrent inventory reservation,
- idempotent integrations,
- auditable state changes,
- secure tenant isolation,
- observable request/event flows,
- resilient asynchronous processing,
- reproducible deployment,
- automated testing and CI/CD.

## 14. Early-phase non-goals

Do not build in Phase 0/1:
- customer storefront,
- shopping cart,
- checkout,
- production payment processing,
- microservices,
- Kafka,
- Redis,
- Kubernetes,
- ML optimization.
