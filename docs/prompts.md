# prompts.md

> Long-form prompts used to generate the project with Cursor. They are intentionally detailed and follow a layered Spring Boot architecture.

---

## PROMPT 1 — store-service: Basic READS + JSON pretty-print

Act as a Spring Boot architect. Implement Iteration 1 of the MVP in the "store-service" project (base package `com.inventory.store`), focused ONLY on READS from H2 in-memory. Use a layered architecture and return JSON responses with pretty-print.

### Objectives
- Service runs on port **8081**
- Expose health and product/stock READ endpoints
- Observability basics: trace-id + uniform error handling
- JSON responses indented for readability
- No writes or sync yet

### Package structure
```
src/main/java/com/inventory/store/
 ├── controller        # REST endpoints (HealthController, ProductController, StockController)
 ├── service           # Business logic (ProductService, StockService)
 ├── repository        # Spring Data JPA repositories
 ├── entity            # JPA entities
 ├── dto               # DTOs
 ├── config            # Infrastructure / filters (TraceFilter)
 ├── exception         # Exceptions + GlobalExceptionHandler
 └── init              # Initial data loader
```

### Configuration (application.yml)
```yaml
  server:
    port: 8081
  spring:
    datasource:
      url: jdbc:h2:mem:storedb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create
    open-in-view: false
  h2:
    console:
      enabled: true
      path: /h2
  jackson:
    serialization:
      indent_output: true
logging:
  level:
    root: INFO
    org.hibernate.SQL: WARN
```

### Entities
- ProductEntity: id (String @Id), name, price (BigDecimal), updatedAt (Instant)
- StockEntity: productId (String @Id), quantity (int), updatedAt (Instant)

### Repositories
- ProductRepository extends JpaRepository<ProductEntity, String>
- StockRepository extends JpaRepository<StockEntity, String>

### Services
- ProductService.findAll()
- StockService.getSnapshot(productId) → returns StockSnapshotDTO or throws NotFoundException

### DTO
- StockSnapshotDTO { productId, quantity, updatedAt }

### Controllers
- HealthController → GET `/health`
- ProductController → GET `/products`
- StockController → GET `/stock/{productId}`

### DataLoader
- Preload electronics products:
  - ABC-001 Laptop Lenovo ThinkPad X1 (1500.00, stock 12)
  - ABC-002 Samsung Galaxy S23 (899.99, stock 30)
  - ABC-003 Sony WH-1000XM5 (349.99, stock 20)

### Error handling
- NotFoundException
- GlobalExceptionHandler → returns JSON: { traceId, path, message, code }

### Observability
- TraceFilter → read/generate X-Trace-Id, put in MDC, return in response

### Manual validation
- GET /health
- GET /products
- GET /stock/ABC-001

---

## PROMPT 2 — store-service: POST /stock/adjust + optimistic locking + change log

Implement Iteration 2 in store-service: add atomic stock adjustments with optimistic locking and an outbox (change_log).

### Endpoint
- POST /stock/adjust
- Request DTO: StockAdjustRequestDTO { productId, delta }
- Response: StockSnapshotDTO

### Entities
- Add @Version to StockEntity
- Create ChangeLogEntity { id(UUID), productId, updatedAt }

### Service
- StockService.adjust(productId, delta)
  - Rules: validate product, ensure newQty >= 0, update quantity & updatedAt
  - Insert ChangeLogEntity
  - Retry optimistic lock up to 3 times (50, 100, 150 ms)

### Controller
- Add POST /stock/adjust

### Errors
- BadRequestException → 400
- NotFoundException → 404

### Logging
- Log start (productId, delta), success (newQty, updatedAt), errors

### Manual validation (curl)
- Test positive, negative, invalid, nonexistent product

---

## PROMPT 3 — central-service: Reads + /sync/pull with LWW

Implement Iteration 3 in central-service with LWW resolution.

### Endpoints
- GET /health
- GET /products
- GET /stock/{productId}
- POST /sync/pull (apply Last-Write-Wins by updatedAt)

### Services
- SyncService.applyBatchLWW(batch)
  - If not exists → create
  - If incoming.updatedAt > current → update
  - Else → skip

### DTOs
- StockSnapshotDTO
- SyncBatchDTO
- SyncResultDTO { received, applied, skipped }

### Logging
- Log summary: received/applied/skipped

---

## PROMPT 4 — store-service: /sync/push + scheduler + retries

Implement /sync/push in store-service with periodic scheduler.

### Features
- Manual endpoint: POST /sync/push
- Scheduler: every 15 minutes
- Retries: up to 3 attempts with backoff
- Clean change_log on success

### DTOs
- SyncBatchDTO, SyncResultDTO

### Service
- SyncPushService.pushNow()
- Build batch from change_log
- Push to central /sync/pull
- Retry on network failure

### Config (application.yml)
- Properties under store.sync.*

---

## PROMPT 5 — Tests: store-service

Add test suite for store-service.

- StockServiceTest → adjust positive, negative, invalid, not found
- WebMvcTest for /health, /products, /stock/{id}
- ConcurrencyAdjustIT → simulate parallel updates with optimistic locking

---

## PROMPT 6 — Tests: central-service

Add test suite for central-service.

- SyncServiceTest → applied, skipped, created, equal timestamps
- Web tests for /sync/pull with MockMvc
- Integration test SyncPullIT → check DB state

---

## PROMPT 7 — Metrics with Micrometer & Actuator

Add metrics to both services.

### store-service
  - inventory_stock_adjust_attempts_total
  - inventory_stock_adjust_success_total
  - inventory_stock_adjust_failed_total
- inventory_stock_adjust_duration_seconds
  - inventory_sync_push_attempts_total
  - inventory_sync_push_success_total
  - inventory_sync_push_failed_total
  - inventory_sync_push_items_applied_total
  - inventory_sync_push_items_skipped_total
- inventory_sync_push_duration_seconds

### central-service
  - inventory_sync_pull_received_total
  - inventory_sync_pull_applied_total
  - inventory_sync_pull_skipped_total
- inventory_sync_pull_duration_seconds

### Actuator
- Expose health, info, metrics, prometheus
- Use MeterRegistry counters and timers in services

---

## PROMPT 8 — Docker & Docker Compose

Add Docker support to this project so that both services (store-service and central-service) can run with Docker Compose.

Tasks:

1) Create a Dockerfile in store-service:
   - Multi-stage build
   - Stage 1: FROM maven:3.9.6-eclipse-temurin-21 as build
     - Copy pom.xml, download deps
     - Copy src, run mvn clean package -DskipTests
   - Stage 2: FROM eclipse-temurin:21-jre
     - Copy JAR from build
     - EXPOSE 8081
     - ENV JAVA_OPTS="-Xms256m -Xmx512m"
     - ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

2) Create a Dockerfile in central-service:
   - Same multi-stage structure
   - Runtime container exposes port 8080

3) Create a compose.yaml in project root:
   - Service "central":
     - build context: ./central-service
     - container_name: central-service
     - ports: "8080:8080"
     - environment: JAVA_OPTS=-Xms256m -Xmx512m
     - healthcheck using GET http://localhost:8080/health
   - Service "store":
     - build context: ./store-service
     - container_name: store-service
     - ports: "8081:8081"
     - environment:
       - JAVA_OPTS=-Xms256m -Xmx512m
       - STORE_SYNC_CENTRAL_BASE_URL=http://central:8080
     - depends_on: central with condition: service_healthy
     - healthcheck using GET http://localhost:8081/health

4) Update store-service application.yml so that:
   store.sync.centralBaseUrl=${STORE_SYNC_CENTRAL_BASE_URL:http://localhost:8080}

5) Do not remove Maven support; Docker should be optional.
6) Do not change code logic, only add Dockerfiles, compose.yaml, and adjust config for centralBaseUrl mapping.

Finally, update run.md:
- Add a section "Run with Docker" at the end.
- Document build and run:
  docker compose build
  docker compose up -d
  docker compose logs -f store
- Mention H2 Console URLs (http://localhost:8081/h2, http://localhost:8080/h2).
- Explain that this is optional; Maven run still works.

---

## PROMPT 9 — store-service: Reservation-based flow (allocate/commit/release) + idempotency

Implement a minimal purchase flow based on reservations in `store-service`, preserving LWW and optimistic locking.

### Model
- Extend `StockEntity` with `onHand:int`, `allocated:int`, `updatedAt:Instant`, and `@Version`.
- Create `idempotency_request` table to persist `Idempotency-Key` uses.

### Service
- Add atomic operations with optimistic retries (or PESSIMISTIC_WRITE if aligned with existing code):
  - `allocate(productId, orderId, quantity)` → rule: `onHand - allocated >= quantity`; effect: `allocated += quantity; updatedAt=now; change_log`.
  - `commit(productId, orderId, quantity)` → effect: `onHand -= quantity; allocated -= quantity; updatedAt=now; change_log`.
  - `release(productId, orderId, quantity)` → effect: `allocated -= quantity; updatedAt=now; change_log`.
- Idempotency via optional `Idempotency-Key` header persisted in `idempotency_request`.

### API
- `POST /stock/allocate` (headers: `Idempotency-Key` optional)
- `POST /stock/commit`
- `POST /stock/release`

### Testing
- Unit tests for allocate/commit/release including validations and concurrency basics.
- WebMvc tests for new endpoints.

### Compatibility
- Do not break existing `/stock/adjust` or `/sync/push`.
- Ensure `change_log` records allocate/commit/release so central reflects via LWW.
