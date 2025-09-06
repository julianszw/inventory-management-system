package com.inventory.central.service;

import com.inventory.central.dto.StockSnapshotDTO;
import com.inventory.central.dto.SyncBatchDTO;
import com.inventory.central.dto.SyncResultDTO;
import com.inventory.central.entity.StockEntity;
import com.inventory.central.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class SyncService {
    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final StockRepository stockRepository;
    private final MeterRegistry meterRegistry;
    private final Counter pullReceived;
    private final Counter pullApplied;
    private final Counter pullSkipped;
    private final Timer pullTimer;

    public SyncService(StockRepository stockRepository, MeterRegistry meterRegistry) {
        this.stockRepository = stockRepository;
        this.meterRegistry = meterRegistry;
        this.pullReceived = Counter.builder("inventory_sync_pull_received_total").register(meterRegistry);
        this.pullApplied = Counter.builder("inventory_sync_pull_applied_total").register(meterRegistry);
        this.pullSkipped = Counter.builder("inventory_sync_pull_skipped_total").register(meterRegistry);
        this.pullTimer = Timer.builder("inventory_sync_pull_duration_seconds").publishPercentileHistogram(true).register(meterRegistry);
    }

    @Transactional
    public SyncResultDTO applyBatchLWW(SyncBatchDTO batch) {
        int received = 0;
        int applied = 0;
        int skipped = 0;
        Timer.Sample sample = Timer.start(meterRegistry);
        if (batch.getItems() != null) {
            for (StockSnapshotDTO item : batch.getItems()) {
                received++;
                StockEntity existing = stockRepository.findById(item.getProductId()).orElse(null);
                if (existing == null) {
                    StockEntity created = StockEntity.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .updatedAt(nonNullInstant(item.getUpdatedAt()))
                            .build();
                    stockRepository.save(created);
                    applied++;
                } else {
                    Instant incoming = nonNullInstant(item.getUpdatedAt());
                    if (incoming.isAfter(existing.getUpdatedAt())) {
                        existing.setQuantity(item.getQuantity());
                        existing.setUpdatedAt(incoming);
                        stockRepository.save(existing);
                        applied++;
                    } else {
                        skipped++;
                    }
                }
            }
        }
        pullReceived.increment(received);
        pullApplied.increment(applied);
        pullSkipped.increment(skipped);
        sample.stop(pullTimer);
        String traceId = MDC.get("traceId");
        log.info("sync received={} applied={} skipped={} traceId={}", received, applied, skipped, traceId);
        return SyncResultDTO.builder().received(received).applied(applied).skipped(skipped).build();
    }

    private Instant nonNullInstant(Instant value) {
        return value != null ? value : Instant.EPOCH;
    }
}


