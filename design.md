# Design Document (Project Plan)

## Context & Problem
- Periodic sync (15 min) causes stale stock and user-visible inconsistencies.
- Monolith + legacy front-end limit evolvability.

## Goals (MVP)
- Two services: `store-service` and `central-service`.
- Availability-first (AP) with **eventual consistency**.
- Conflict resolution via **LWW** (by `updatedAt`).
- Concurrency safety local (optimistic locking + retries).
- Observability: metrics, health, trace-id.

## Architecture & Rationale
- **AP-first**: each store must accept operations without blocking on the central service.
- **LWW**: simple and effective; assumes stores stamp events with `updatedAt`.
- **Outbox/Change Log** in store → ensures later, retriable delivery.
- **Retries**: handle network/transient failures in push/pull.
- **H2 in-memory**: low cost and fast for MVP (dev/test).

## Trade-offs (Consistency vs Availability)
- **AP advantage**: low local latency, high availability.
- **Cost**: potential simultaneous overwrite; LWW can lose updates.
- **Future mitigations**:
  - Idempotency keys per event.
  - Version keys or vector clocks to detect lost updates.
  - Event streaming (Kafka) and CQRS for reconciliation.

## Latency Strategy
- **Now**: 15-min cron + **manual push**.
- **Improvements**:
  - Shorter cron (1–5 min), **adaptive backoff** on failures.
  - **Push on change** with debouncing (e.g., every 2–5s per burst).
  - Near real-time with queues/events (Kafka) in the next version.

## Cost Considerations
- Low dev cost (H2, no brokers).
- Simple deployment: two services, independently scalable.
- Metrics → focus on savings by reducing inconsistencies (fewer lost sales).

## Security (MVP)
- **No auth/TLS** for simplicity.
- Next steps:
  - mTLS/HTTPS and auth token (Bearer/JWT).
  - Rate limiting per IP/endpoint.
  - Hardened input validation/normalization.
  - Secret management.

## Observability
- **Metrics**: stock adjust/sync push/pull; timers and counters.
- **Tracing**: `X-Trace-Id` propagated.
- **Logs**: key events and errors with context.

## Testing
- Unit, web, integration (concurrency and LWW).
- Stabilization in CI/IntelliJ.

## Roadmap (post-MVP)
- Streaming/event-driven, idempotency, reconciliation job.
- Separate product catalog.
- Security hardening and observable SLOs.
