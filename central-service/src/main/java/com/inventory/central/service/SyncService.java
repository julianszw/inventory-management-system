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

@Service
public class SyncService {
    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final StockRepository stockRepository;

    public SyncService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Transactional
    public SyncResultDTO applyBatchLWW(SyncBatchDTO batch) {
        int received = 0;
        int applied = 0;
        int skipped = 0;

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

        String traceId = MDC.get("traceId");
        log.info("sync received={} applied={} skipped={} traceId={}", received, applied, skipped, traceId);
        return SyncResultDTO.builder().received(received).applied(applied).skipped(skipped).build();
    }

    private Instant nonNullInstant(Instant value) {
        return value != null ? value : Instant.EPOCH;
    }
}


