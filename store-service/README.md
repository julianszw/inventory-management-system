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

