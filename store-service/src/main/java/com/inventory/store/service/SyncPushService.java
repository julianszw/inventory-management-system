package com.inventory.store.service;

import com.inventory.store.dto.StockSnapshotDTO;
import com.inventory.store.dto.SyncBatchDTO;
import com.inventory.store.dto.SyncResultDTO;
import com.inventory.store.entity.ChangeLogEntity;
import com.inventory.store.entity.StockEntity;
import com.inventory.store.exception.SyncNetworkException;
import com.inventory.store.repository.ChangeLogRepository;
import com.inventory.store.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class SyncPushService {
    private static final Logger log = LoggerFactory.getLogger(SyncPushService.class);

    private final ChangeLogRepository changeLogRepository;
    private final StockRepository stockRepository;
    private final CentralSyncClient centralSyncClient;

    private final int maxRetries;
    private final long initialBackoffMs;

    public SyncPushService(ChangeLogRepository changeLogRepository,
                           StockRepository stockRepository,
                           CentralSyncClient centralSyncClient,
                           @Value("${store.sync.maxRetries:3}") int maxRetries,
                           @Value("${store.sync.initialBackoffMs:200}") long initialBackoffMs) {
        this.changeLogRepository = changeLogRepository;
        this.stockRepository = stockRepository;
        this.centralSyncClient = centralSyncClient;
        this.maxRetries = maxRetries;
        this.initialBackoffMs = initialBackoffMs;
    }

    public SyncBatchDTO buildBatchSinceLastPush() {
        List<ChangeLogEntity> changes = changeLogRepository.findAll();
        if (changes.isEmpty()) {
            return SyncBatchDTO.builder().items(Collections.emptyList()).build();
        }
        // Obtener productIds únicos
        Set<String> productIds = new HashSet<>();
        for (ChangeLogEntity ch : changes) {
            productIds.add(ch.getProductId());
        }
        List<StockSnapshotDTO> items = new ArrayList<>();
        for (String productId : productIds) {
            Optional<StockEntity> maybe = stockRepository.findById(productId);
            if (maybe.isPresent()) {
                StockEntity st = maybe.get();
                items.add(StockSnapshotDTO.builder()
                        .productId(st.getProductId())
                        .quantity(st.getQuantity())
                        .updatedAt(st.getUpdatedAt())
                        .build());
            }
        }
        return SyncBatchDTO.builder().items(items).build();
    }

    public SyncResultDTO pushNow() {
        long start = System.currentTimeMillis();
        SyncBatchDTO batch = buildBatchSinceLastPush();
        String traceId = MDC.get("traceId");
        int toPush = batch.getItems() == null ? 0 : batch.getItems().size();
        log.info("[traceId={}] sync push inicio: items={} ", traceId, toPush);

        if (toPush == 0) {
            log.info("[traceId={}] sync push no-op (sin cambios)", traceId);
            return SyncResultDTO.builder().received(0).applied(0).skipped(0).build();
        }

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                SyncResultDTO result = centralSyncClient.pushBatch(batch);
                changeLogRepository.deleteAll();
                long duration = System.currentTimeMillis() - start;
                log.info("[traceId={}] sync push ok: received={} applied={} skipped={} durationMs={}",
                        traceId, result.getReceived(), result.getApplied(), result.getSkipped(), duration);
                return result;
            } catch (SyncNetworkException ex) {
                if (attempt >= maxRetries) {
                    long duration = System.currentTimeMillis() - start;
                    log.error("[traceId={}] sync push error final: intentos={} durationMs={} causa={}", traceId, attempt, duration, ex.getMessage());
                    throw ex;
                }
                long sleepMs = initialBackoffMs * attempt;
                log.warn("[traceId={}] sync push error: intento={} backoffMs={} causa={}", traceId, attempt, sleepMs, ex.getMessage());
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SyncNetworkException("Interrumpido durante backoff de reintento", ie);
                }
            }
        }
    }
}


