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
