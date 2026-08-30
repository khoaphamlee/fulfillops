# FulfillOps — Roadmap

The roadmap controls when technologies are allowed.

## Phase 0 — Project Foundation
- repository structure
- documentation
- AGENTS.md
- Git workflow
- Codex working contract

## Phase 1 — Backend Foundation
Technology:
- Java
- Spring Boot
- Maven
- REST
- validation
- OpenAPI
- PostgreSQL
- Flyway
- JPA/Hibernate
- Docker Compose for local PostgreSQL
- basic testing

Business:
- tenant
- membership
- warehouse
- location hierarchy
- SKU
- inbound
- inventory foundation

## Phase 2 — Warehouse Execution & Fulfillment
- inventory ledger
- receiving
- putaway
- transfers
- cycle count
- fulfillment request
- allocation
- reservation
- concurrency
- picking
- packing
- shipment creation

## Phase 3 — Security & SaaS Hardening
- Spring Security
- access/refresh token
- RBAC
- tenant authorization
- tenant isolation tests
- audit
- plan/quota foundations

## Phase 4 — Redis & Background Jobs
- reservation TTL/temporary state
- rate limiting
- caching
- idempotency helpers where justified
- expired reservation jobs
- SLA/stale-work checks

## Phase 5 — Transportation & Realtime
- driver
- vehicle
- dispatch
- tracking
- delivery
- failed delivery
- proof of delivery
- returns
- WebSocket/SSE
- optional PostGIS
- MinIO/S3

## Phase 6 — Event-Driven Architecture
- Kafka
- event schemas
- retries
- DLQ
- idempotent consumers
- transactional outbox

## Phase 7 — Integrations & Search
- external fulfillment API
- signed webhooks
- retry/replay
- OpenSearch when justified

## Phase 8 — Observability & Performance
- Spring Actuator
- OpenTelemetry
- Prometheus
- Grafana
- Loki
- Tempo
- load testing
- query/JVM tuning

## Phase 9 — CI/CD & Cloud
- Docker
- GitHub Actions
- registry
- AWS
- managed PostgreSQL
- S3
- networking
- HTTPS
- secrets

## Phase 10 — Infrastructure as Code
- Terraform

## Phase 11 — Service Extraction
- API Gateway
- selected microservices
- REST/gRPC where appropriate
- Kafka async communication
- Resilience4j
- distributed tracing

## Phase 12 — Kubernetes
- Kubernetes
- Helm
- probes
- rolling deployment
- autoscaling basics
