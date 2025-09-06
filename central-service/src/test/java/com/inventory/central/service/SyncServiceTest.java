package com.inventory.central.service;

import com.inventory.central.dto.StockSnapshotDTO;
import com.inventory.central.dto.SyncBatchDTO;
import com.inventory.central.dto.SyncResultDTO;
import com.inventory.central.entity.StockEntity;
import com.inventory.central.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SyncServiceTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setup() {
        stockRepository.deleteAll();
        stockRepository.save(StockEntity.builder()
                .productId("ABC-001")
                .quantity(10)
                .updatedAt(Instant.parse("2025-01-01T00:00:00Z"))
                .build());
    }

    @Test
    void applied_when_incoming_is_newer() {
        SyncBatchDTO batch = SyncBatchDTO.builder().items(List.of(
                StockSnapshotDTO.builder().productId("ABC-001").quantity(15).updatedAt(Instant.parse("2025-02-01T00:00:00Z")).build()
        )).build();
        SyncResultDTO result = syncService.applyBatchLWW(batch);
        assertThat(result.getApplied()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);
        StockEntity updated = stockRepository.findById("ABC-001").orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(15);
        assertThat(updated.getUpdatedAt()).isEqualTo(Instant.parse("2025-02-01T00:00:00Z"));
    }

    @Test
    void skipped_when_incoming_is_older() {
        SyncBatchDTO batch = SyncBatchDTO.builder().items(List.of(
                StockSnapshotDTO.builder().productId("ABC-001").quantity(1).updatedAt(Instant.parse("2024-12-01T00:00:00Z")).build()
        )).build();
        SyncResultDTO result = syncService.applyBatchLWW(batch);
        assertThat(result.getApplied()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        StockEntity existing = stockRepository.findById("ABC-001").orElseThrow();
        assertThat(existing.getQuantity()).isEqualTo(10);
    }

    @Test
    void create_when_not_exists() {
        SyncBatchDTO batch = SyncBatchDTO.builder().items(List.of(
                StockSnapshotDTO.builder().productId("ABC-999").quantity(7).updatedAt(Instant.parse("2025-02-01T00:00:00Z")).build()
        )).build();
        SyncResultDTO result = syncService.applyBatchLWW(batch);
        assertThat(result.getApplied()).isEqualTo(1);
        StockEntity created = stockRepository.findById("ABC-999").orElseThrow();
        assertThat(created.getQuantity()).isEqualTo(7);
    }

    @Test
    void mixed_batch_applied_and_skipped() {
        stockRepository.save(StockEntity.builder()
                .productId("ABC-002")
                .quantity(20)
                .updatedAt(Instant.parse("2025-03-01T00:00:00Z"))
                .build());

        SyncBatchDTO batch = SyncBatchDTO.builder().items(List.of(
                StockSnapshotDTO.builder().productId("ABC-001").quantity(12).updatedAt(Instant.parse("2025-02-01T00:00:00Z")).build(),
                StockSnapshotDTO.builder().productId("ABC-002").quantity(99).updatedAt(Instant.parse("2025-02-15T00:00:00Z")).build()
        )).build();
        SyncResultDTO result = syncService.applyBatchLWW(batch);
        assertThat(result.getReceived()).isEqualTo(2);
        assertThat(result.getApplied()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
    }

    @Test
    void same_timestamp_treated_as_skipped() {
        SyncBatchDTO batch = SyncBatchDTO.builder().items(List.of(
                StockSnapshotDTO.builder().productId("ABC-001").quantity(99).updatedAt(Instant.parse("2025-01-01T00:00:00Z")).build()
        )).build();
        SyncResultDTO result = syncService.applyBatchLWW(batch);
        assertThat(result.getApplied()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(1);
        StockEntity existing = stockRepository.findById("ABC-001").orElseThrow();
        assertThat(existing.getQuantity()).isEqualTo(10);
    }
}


