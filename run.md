# Inventory Management System – Run Guide

A distributed system composed of two independent Spring Boot applications:
- store-service (port 8081): local store inventory with reads, atomic stock writes with optimistic locking, change outbox, periodic/manual sync push to central, and Micrometer metrics.
- central-service (port 8080): central aggregator exposing reads and accepting sync pull batches using Last-Write-Wins (LWW) conflict resolution, with Micrometer metrics.

## Requirements
- Java 21 (JDK 21)
- Maven 3.9+
- Optional clients: Postman, Thunder Client, or curl/PowerShell

## How to Run
### Option A: Run with Maven (recommended for dev)
Open two terminals.

Terminal 1 – store-service
```bash
cd store-service
./mvnw spring-boot:run
```
PowerShell on Windows:
```powershell
cd store-service; .\mvnw spring-boot:run
```

Terminal 2 – central-service
```bash
cd central-service
./mvnw spring-boot:run
```
PowerShell on Windows:
```powershell
cd central-service; .\mvnw spring-boot:run
```

### Option B: Run packaged JARs
Build once from the repo root, then run each service.

Build
```bash
./mvnw -DskipTests package
```
PowerShell:
```powershell
.\mvnw -DskipTests package
```
Run store-service (Terminal 1)
```bash
java -jar store-service/target/store-service-0.0.1-SNAPSHOT.jar
```
Run central-service (Terminal 2)
```bash
java -jar central-service/target/central-service-0.0.1-SNAPSHOT.jar
```

## Default Configuration
- Ports: 8081 (store-service), 8080 (central-service)
- Persistence: H2 in-memory databases
- Scheduler (store-service): sync push every 15 minutes (configurable)
  - Properties (store-service `application.yml`):
    - `store.sync.centralBaseUrl` (default `http://localhost:8080`)
    - `store.sync.enabled` (default `true`)
    - `store.sync.fixedDelayMs` (default `900000` = 15 min)
    - `store.sync.maxRetries` (default `3`)
    - `store.sync.initialBackoffMs` (default `200` ms)
- JSON: pretty-print enabled
- Actuator: health, info, metrics, prometheus (if Prometheus registry is on classpath)

## Main Endpoints
### store-service (8081)
- GET `/health`
- GET `/products`
- GET `/stock/{productId}`
- POST `/stock/adjust`
  - Example body:
```json
{
  "productId": "ABC-001",
  "delta": 5
}
```
- POST `/sync/push` (push local changes to central)

### central-service (8080)
- GET `/health`
- GET `/products`
- GET `/stock/{productId}`
- POST `/sync/pull`
  - Example body:
```json
{
  "items": [
    { "productId": "ABC-001", "quantity": 12, "updatedAt": "2025-09-06T15:02:00Z" },
    { "productId": "ABC-002", "quantity": 30, "updatedAt": "2025-09-06T15:03:00Z" }
  ]
}
```

## Quick Test Examples (PowerShell)
Reading endpoints
```powershell
Invoke-RestMethod -Uri http://localhost:8081/health
Invoke-RestMethod -Uri http://localhost:8081/products
Invoke-RestMethod -Uri http://localhost:8081/stock/ABC-001
Invoke-RestMethod -Uri http://localhost:8080/health
```
Adjust stock then push changes
```powershell
# Adjust in store
Invoke-RestMethod -Uri http://localhost:8081/stock/adjust -Method Post -ContentType "application/json" -Body '{"productId":"ABC-002","delta":3}'
# Push to central
Invoke-RestMethod -Uri http://localhost:8081/sync/push -Method Post
```
Alternative with curl.exe on Windows
```powershell
curl.exe -s http://localhost:8081/health
curl.exe -s -H "Content-Type: application/json" -d '{"productId":"ABC-001","delta":2}' http://localhost:8081/stock/adjust
curl.exe -s -X POST http://localhost:8081/sync/push
```

## H2 Console
- store-service: `http://localhost:8081/h2`
  - JDBC URL: `jdbc:h2:mem:storedb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
  - Username: `sa`  Password: (blank)
- central-service: `http://localhost:8080/h2`
  - JDBC URL: `jdbc:h2:mem:centraldb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL`
  - Username: `sa`  Password: (blank)

## Observability and Metrics
- Tracing: send/receive header `X-Trace-Id`; both services log method, path, status, and duration.
- Actuator endpoints
  - `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/metrics/{metric}`
  - `/actuator/prometheus` (if Prometheus registry is present)
- Custom metrics (Micrometer)
  - store-service
    - `inventory_stock_adjust_attempts_total`
    - `inventory_stock_adjust_success_total`
    - `inventory_stock_adjust_failed_total`
    - `inventory_stock_adjust_duration_seconds`
    - `inventory_sync_push_attempts_total`
    - `inventory_sync_push_success_total`
    - `inventory_sync_push_failed_total`
    - `inventory_sync_push_items_applied_total`
    - `inventory_sync_push_items_skipped_total`
    - `inventory_sync_push_duration_seconds`
  - central-service
    - `inventory_sync_pull_received_total`
    - `inventory_sync_pull_applied_total`
    - `inventory_sync_pull_skipped_total`
    - `inventory_sync_pull_duration_seconds`

## Key Design Decisions
- AP-first with eventual consistency
- Periodic + manual sync push from store to central
- LWW (Last-Write-Wins) conflict resolution on central by `updatedAt`
- Optimistic locking + retries for local stock writes in store
- Network retries with backoff for sync push

## Troubleshooting
- Port already in use
  - Change server port in the corresponding `application.yml` (`server.port`), or stop the process using the port.
- PowerShell curl alias issues
  - Use `curl.exe` explicitly, or `Invoke-RestMethod` for JSON-friendly output.
- Sync not working
  - Ensure central-service is running and reachable at `store.sync.centralBaseUrl`.
  - Check store logs for `SERVICE_UNAVAILABLE` errors and retry messages.
  - Verify outbox `change_log` has entries before push; it clears after a successful push.
- H2 Console access
  - Visit `/h2` on each service and use the JDBC URLs above. If schema is empty, ensure the service has started with `ddl-auto: create` and initial loaders ran.
