# Distributed Inventory Management System

This repository contains a **prototype MVP** of a distributed inventory management system.  
It demonstrates how multiple **store services** synchronize stock data with a **central service**, prioritizing **availability** while ensuring **eventual consistency**.

---

## 🏗 Architecture Overview

```
+-------------------+       Sync Push (every 15 min / manual)       +-------------------+
|                   |  ----------------------------------------->  |                   |
|   store-service   |                                              |  central-service  |
|   (port 8081)     |   <----------------------------------------- |   (port 8080)     |
|                   |          Pull response (applied/skipped)     |                   |
+-------------------+                                              +-------------------+
         |                                                                 |
         |  Local adjustments (/stock/adjust)                              | 
         |  with optimistic locking and change log                         |
         v                                                                 v
  H2 in-memory DB                                                  H2 in-memory DB
```

- **store-service**
  - Exposes endpoints to read products and adjust stock.
  - Keeps an outbox (`change_log`) of changes.
  - Pushes changes periodically (scheduler) or manually to the central service.
  - Retries on DB lock conflicts (optimistic locking) and on network failures.

- **central-service**
  - Receives updates via `/sync/pull`.
  - Applies **Last-Write-Wins (LWW)** based on `updatedAt`.
  - Exposes endpoints to read consolidated inventory.

---

## ⚙️ Technology Stack
- Java 21
- Spring Boot 3.5.5
- Spring Data JPA + H2 in-memory DB
- Spring Boot Actuator + Micrometer (metrics)
- Lombok
- JUnit 5, AssertJ, Mockito (tests)

---

## 📡 Main Endpoints

### store-service (port 8081)
- `GET /health`
- `GET /products`
- `GET /stock/{productId}`
- `POST /stock/adjust`
- `POST /sync/push`

### central-service (port 8080)
- `GET /health`
- `GET /products`
- `GET /stock/{productId}`
- `POST /sync/pull`

---

## 📊 Metrics (Actuator)

### store-service
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

### central-service
- `inventory_sync_pull_received_total`
- `inventory_sync_pull_applied_total`
- `inventory_sync_pull_skipped_total`
- `inventory_sync_pull_duration_seconds`

---

## 🚀 How to Run

See [run.md](./run.md) for detailed instructions.  
Summary:

```bash
# Terminal 1
cd store-service
mvn -q -DskipTests spring-boot:run

# Terminal 2
cd central-service
mvn -q -DskipTests spring-boot:run
```

Docker (optional): you can also run both services with Docker Compose. See the "Run with Docker" section in [run.md](./run.md) for `docker compose build` and `docker compose up -d` commands.

Then test:
```bash
Invoke-RestMethod http://localhost:8081/products
Invoke-RestMethod http://localhost:8080/products
```

Postman collection: import `Inventory_Distributed_System.postman_collection.json` and `Inventory_Local.postman_environment.json` from the project root. Optionally send the `X-Trace-Id` header to correlate logs.

---

## 🧪 Testing

- **store-service**
  - Unit tests for `StockService`
  - Web tests for controllers (`/health`, `/products`, `/stock`)
  - Concurrency integration test (multiple adjust calls in parallel)

- **central-service**
  - Unit tests for `SyncService` (LWW: applied, skipped, created, equal timestamps)
  - Web tests for `/sync/pull` (valid, invalid requests)
  - Integration tests verifying database state after pulls

Run all tests:
```bash
mvn -q test
```

---

## 🔑 Key Design Decisions
- **Eventual consistency** via scheduled push (every 15 minutes).
- **Availability over strict consistency** (AP-first).
- **Conflict resolution**: Last-Write-Wins (based on `updatedAt`).
- **Local concurrency**: optimistic locking with retries.
- **Fault tolerance**: retries on DB locks and network failures.
- **Observability**: request tracing (`X-Trace-Id`) + metrics via Actuator.

---

## 📂 Additional Files
- [run.md](./run.md) — Detailed run instructions and troubleshooting
- [prompts.md](./docs/prompts.md) — Full prompts used in Cursor to generate this project
- [genai_workflow.md](./docs/genai_workflow.md) — How GenAI tools (ChatGPT/Cursor) were used in design and implementation
- [technical_strategy.md](./docs/technical_strategy.md) — Technology stack choices, rationale, trade-offs, and tooling strategy
- [design.md](./docs/design.md) — High-level design and project plan (architecture, trade-offs, latency strategy, security, observability, roadmap)

## Postman

The Postman collection and environment are provided in the project root:

- `Inventory_Distributed_System.postman_collection.json`
- `Inventory_Local.postman_environment.json`

### Usage
1. Open Postman
2. Import both files (File → Import)
3. Select the environment `Inventory Local` in the top right corner
4. Run the requests (health, stock adjust, sync, metrics) directly
