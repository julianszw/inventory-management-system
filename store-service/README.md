# Store Service

## Iteración 1 - Lecturas

Servicio Spring Boot con arquitectura en capas, sólo lecturas desde H2 en memoria.

- Puerto: 8081
- Endpoints:
  - GET /health → {"status":"UP"}
  - GET /products → lista de productos (desde H2)
  - GET /stock/{productId} → snapshot de stock
- Observabilidad: header `X-Trace-Id` de entrada/salida, logs con método, path, status, duración.
- Manejo de errores uniforme: JSON con `traceId`, `path`, `message`, `code`.

### Configuración
Relevante en `src/main/resources/application.yml`:
- H2 en memoria `jdbc:h2:mem:storedb`
- JPA `ddl-auto: create`
- Consola H2 en `/h2`

### Datos iniciales
- ABC-001 → Laptop Lenovo ThinkPad X1 → 1500.00 → stock 12
- ABC-002 → Smartphone Samsung Galaxy S23 → 899.99 → stock 30
- ABC-003 → Auriculares Sony WH-1000XM5 → 349.99 → stock 20

### Correr
```bash
./mvnw spring-boot:run
```

### Curl de validación
```bash
curl -s -H "X-Trace-Id: demo-123" http://localhost:8081/health
curl -s -H "X-Trace-Id: demo-123" http://localhost:8081/products
curl -s -H "X-Trace-Id: demo-123" http://localhost:8081/stock/ABC-001
```

## Sincronización tienda → central

- Endpoint manual: `POST /sync/push` envía al central los cambios locales acumulados (outbox `change_log`).
- Scheduler: cada 15 minutos (configurable) intenta push automático si `store.sync.enabled=true`.
- Propiedades relevantes en `application.yml`:
```
store:
  sync:
    centralBaseUrl: http://localhost:8080
    enabled: true
    fixedDelayMs: 900000
    maxRetries: 3
    initialBackoffMs: 200
```

### Ejemplos (PowerShell)
```powershell
# Generar cambios
Invoke-RestMethod -Uri http://localhost:8081/stock/adjust -Method Post -ContentType "application/json" -Body '{"productId":"ABC-001","delta":5}'

# Disparar push manual
Invoke-RestMethod -Uri http://localhost:8081/sync/push -Method Post

# Sin items (no-op)
Invoke-RestMethod -Uri http://localhost:8081/sync/push -Method Post

# Simular red caída (apagar central) → espera 503 tras reintentos
Invoke-RestMethod -Uri http://localhost:8081/sync/push -Method Post
```

## Iteración 2 - Escrituras y concurrencia local

- Endpoint: `POST /stock/adjust` aplica un `delta` (+/-) al stock de un producto y devuelve `StockSnapshotDTO`.
- Consistencia local: `@Version` en `StockEntity` para bloqueo optimista. Se reintenta hasta 3 veces con backoff simple (50ms, 100ms, 150ms) ante `OptimisticLockException`.
- Resolución de conflictos: última escritura gana por `updatedAt` (LWW) a nivel local.
- Outbox mínimo: tabla `change_log` con `{id, productId, updatedAt}` para futura sincronización tienda→central.

### Curl de validación (Iteración 2)
```bash
curl -X POST http://localhost:8081/stock/adjust -H "Content-Type: application/json" -d '{"productId":"ABC-001","delta":5}'
curl -X POST http://localhost:8081/stock/adjust -H "Content-Type: application/json" -d '{"productId":"ABC-001","delta":-3}'
curl -X POST http://localhost:8081/stock/adjust -H "Content-Type: application/json" -d '{"productId":"ABC-001","delta":-999}'
curl -X POST http://localhost:8081/stock/adjust -H "Content-Type: application/json" -d '{"productId":"NOPE-999","delta":1}'
```

Alcance: no se implementa todavía la sincronización con el servicio central ni endpoints de lectura de `change_log`. Las pruebas se agregan en una iteración posterior.

