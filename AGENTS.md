# FulfillOps Agent Instructions

## 1. Project intent

FulfillOps is a production-oriented, multi-tenant fulfillment and logistics orchestration platform.

The learning goal is as important as the implementation goal. Do not optimize for "fastest code generation". Prefer designs that are explainable, testable, and appropriate for a Java/Spring backend portfolio.

## 2. Source of truth

Before any non-trivial change, read the relevant documents:

- `docs/REQUIREMENTS.md` — business requirements and invariants.
- `docs/ARCHITECTURE.md` — module boundaries and architecture rules.
- `docs/DATABASE.md` — persistence, tenant isolation, transactions, migrations.
- `docs/API_CONVENTIONS.md` — REST API conventions and error contract.
- `docs/SECURITY.md` — authentication, authorization and tenant rules.
- `docs/TESTING.md` — required test strategy.
- `docs/ROADMAP.md` — what technology is allowed in the current phase.
- `docs/TASKS.md` — task IDs, scope and completion status.
- `docs/adr/` — architecture decisions already made.

If documents conflict:
1. Do not silently choose one.
2. Report the conflict.
3. Identify the exact files/sections involved.
4. Ask for a decision before implementing the conflicting behavior.

## 3. Current architecture rule

Start as a modular monolith.

Do NOT introduce microservices, Kafka, Redis, Kubernetes, gRPC, OpenSearch, cloud-specific infrastructure, or distributed-system patterns before the roadmap phase that explicitly allows them.

New technology must solve an actual requirement.

## 4. Change discipline

For each task:

1. Read the task in `docs/TASKS.md`.
2. Inspect existing code before proposing changes.
3. Explain the relevant request/data flow.
4. Produce a short implementation plan.
5. Keep the diff limited to the requested task.
6. Avoid unrelated refactors.
7. Update tests.
8. Update OpenAPI/docs when public behavior changes.
9. Update an ADR when an architectural decision changes.
10. Report commands/tests executed and their results.

Never mark a task complete when required tests are failing.

## 5. Java/Spring rules

When backend code exists:

- Keep controllers thin.
- Put business rules in application/domain services, not controllers.
- Do not expose JPA entities directly as public API responses.
- Use DTOs at API boundaries.
- Use Jakarta Bean Validation for request validation.
- Use explicit transaction boundaries for state-changing business flows.
- Avoid unnecessary bidirectional JPA relationships.
- Watch for N+1 queries.
- Database schema changes must use Flyway.
- Do not rely on Hibernate `ddl-auto=update` as a migration strategy.
- Never log passwords, tokens, secrets, or sensitive credentials.

## 6. Multi-tenancy rules

Tenant isolation is a security boundary.

- Tenant-scoped data must always be associated with a tenant.
- Never trust an arbitrary `tenantId` supplied by the request body/query parameter as authorization proof.
- The active tenant must come from trusted authenticated context/membership resolution.
- Every tenant-scoped read/write must enforce tenant ownership.
- Cross-tenant access tests are mandatory for tenant-scoped resources.
- Cache keys, events, files and search documents must become tenant-aware when those technologies are introduced.

## 7. Inventory correctness rules

Inventory is not a plain mutable `quantity` field.

The system will evolve toward:
- on-hand quantity
- reserved quantity
- available quantity
- inventory movements / ledger
- reservation lifecycle
- auditable adjustments

Do not introduce business behavior that can make inventory negative or allow the same stock to be reserved twice without explicitly addressing concurrency.

## 8. Testing expectations

At minimum:
- Unit tests for non-trivial business rules.
- Integration tests for persistence and API behavior.
- Security tests for authorization.
- Cross-tenant isolation tests for tenant-scoped resources.
- Concurrency tests when reservation/allocation logic is introduced.

Use Testcontainers when the roadmap reaches integration infrastructure. Prefer realistic tests over excessive mocking.

## 9. Definition of done

A task is done only when:
- behavior matches requirements,
- architecture rules are respected,
- tests required by the task pass,
- no known cross-tenant leak exists,
- public API docs are updated when applicable,
- migrations exist for schema changes,
- relevant documentation is updated,
- the final report lists changed files, tests run, results and remaining risks.

## 10. Learning mode

When asked to teach before coding, explain only the concepts needed for the current task:

1. current request/data flow,
2. transaction boundary,
3. SQL/JPA implications,
4. failure modes,
5. security/multi-tenant implications,
6. how tests will prove correctness.

Do not dump unrelated theory.
