# Technical Strategy

## Backend Technology Stack
- **Language & Runtime:** Java 21 (LTS). Modern features (records, pattern matching) and long-term support.
- **Framework:** Spring Boot 3.5.x. Production-ready defaults, Actuator, easy testing.
- **Persistence:** H2 in-memory DB. Fast, zero-ops for a prototyping context; console enabled for inspection.
- **Data Access:** Spring Data JPA with optimistic locking via `@Version` for safe concurrent updates.
- **Serialization:** Jackson with pretty-print enabled for developer-friendly responses.
- **Build Tool:** Maven 3.9.x. Reproducible builds, wrapper optional, ready for CI.
- **Observability:** Spring Boot Actuator + Micrometer. Custom domain metrics for stock adjust and sync flows.
- **HTTP Clients:** Spring WebClient/RestTemplate (as needed) for store → central sync.
- **Containerization (optional):** Docker multi-stage builds + Docker Compose for local orchestration.
- **Testing:** JUnit 5, AssertJ, Mockito, Spring Boot Test (unit, web, and integration tests).

## Rationale & Trade-offs
- **Eventual Consistency (AP-first):** Prioritize store availability; resolve conflicts with **Last-Write-Wins** (`updatedAt`).
- **Optimistic Locking & Retries:** Favor throughput and simpler contention handling for inventory updates.
- **Reservation Flow:** `allocate`/`commit`/`release` adds minimal order support without central coupling.
- **Idempotency:** Optional `Idempotency-Key` on `allocate` to handle client/network retries safely.
- **H2 In-Memory:** Minimizes ops cost and complexity for a challenge/MVP while keeping SQL semantics.
- **Modular Services:** `store-service` and `central-service` make data flows explicit and testable.

Latency & Cost note: This MVP reduces coordination latency by allowing local writes (AP) and batching/periodic sync; costs are kept low with H2 and optional Docker (no external infra). See `design.md` for detailed latency strategy and AP vs CP trade-offs.

## Integration of GenAI and Modern Tooling
- **ChatGPT (design partner):**
  - Explored architecture options (consistency vs availability, optimistic locking, synchronization cadence, metrics, tracing).  
  - Shaped API design and DTO boundaries; validated error handling and observability approach.
  - Iteratively refined long-form prompts to keep generation aligned with layered architecture.
- **Cursor AI (coding assistant):**
  - Applied the refined prompts to generate controllers/services/repositories/tests.
  - Accelerated boilerplate and repetitive patterns while preserving review & control in the IDE.
- **Artifacts produced with GenAI support:**
  - Incremental code for both services, test suites, `run.md`, Postman collection, Dockerfiles/Compose.
  - `genai_workflow.md` documents the process and governance.

## MVP Scope (what is intentionally included/excluded)
- **Included:** core reads/writes, reservation flow (allocate/commit/release), sync push/pull, LWW conflict handling, optimistic locking, metrics, tracing, H2 console, Postman, optional Docker.
- **Excluded:** external databases, message brokers, auth, and multi-region deployment (out of scope for a concise MVP).

## How this improves efficiency
- **Fast feedback loop:** H2 + Actuator + Postman enable rapid iteration.
- **Repeatable runs:** Maven/Compose make local environments deterministic.
- **Lower cognitive load:** Layered structure + DTOs + tests keep the codebase navigable as features grow.
