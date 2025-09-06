# Central Service

## Iteración 3 - Central y LWW

Servicio Spring Boot centralizador: lecturas básicas, escritura via sincronización tipo pull con LWW.

- Puerto: 8080
- Endpoints:
  - GET /health → {"status":"UP"}
  - GET /products → lista de productos
  - GET /stock/{productId} → snapshot de stock
  - POST /sync/pull → aplica LWW sobre lote de snapshots
- Observabilidad: header `X-Trace-Id` entrada/salida, logs con método, path, status y duración.

### Regla LWW (Last-Write-Wins)
Para cada item `{productId, quantity, updatedAt}` recibido:
- Si no existe en central → crear con `quantity` y `updatedAt` → applied
- Si existe y `updatedAt` es mayor a la registrada → actualizar → applied
- En caso contrario → skipped

La respuesta agrega un resumen: `received`, `applied`, `skipped`.

### Ejemplo de body/response
Request:
```json
{
  "items": [
    {"productId":"ABC-001","quantity":12,"updatedAt":"2025-09-06T15:02:00Z"},
    {"productId":"ABC-002","quantity":30,"updatedAt":"2025-09-06T15:03:00Z"}
  ]
}
```

Response:
```json
{"received":2,"applied":2,"skipped":0}
```

### Datos iniciales
- ABC-001 → stock 10
- ABC-002 → stock 28
- ABC-003 → stock 18
`updatedAt = Instant.now()` en el arranque.

### Correr
```bash
./mvnw spring-boot:run
```

### Próxima iteración
Se añadirá `/sync/push` y un scheduler en store-service para enviar periódicamente cambios.

## Métricas
- Principales:
  - inventory_sync_pull_received_total | applied_total | skipped_total | duration_seconds
- Actuator:
  - /actuator/health, /actuator/metrics, /actuator/metrics/{metric}, /actuator/prometheus


