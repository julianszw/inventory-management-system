package com.inventory.central.integration;

import com.inventory.central.dto.StockSnapshotDTO;
import com.inventory.central.dto.SyncBatchDTO;
import com.inventory.central.dto.SyncResultDTO;
import com.inventory.central.entity.StockEntity;
import com.inventory.central.repository.StockRepository;
import com.inventory.central.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SyncPullIT {

    @Autowired
    private SyncService syncService;

    @Autowired
    private StockRepository stockRepository;

    @Test
    void end_to_end_lww_flow() {
        stockRepository.deleteAll();
        stockRepository.save(StockEntity.builder().productId("ABC-001").quantity(10).updatedAt(Instant.parse("2025-01-01T00:00:00Z")).build());

        SyncBatchDTO newer = SyncBatchDTO.builder().items(List.of(
                StockSnapshotDTO.builder().productId("ABC-001").quantity(15).updatedAt(Instant.parse("2025-02-01T00:00:00Z")).build()
        )).build();
        SyncResultDTO r1 = syncService.applyBatchLWW(newer);
        assertThat(r1.getApplied()).isEqualTo(1);

        SyncBatchDTO older = SyncBatchDTO.builder().items(List.of(
                StockSnapshotDTO.builder().productId("ABC-001").quantity(1).updatedAt(Instant.parse("2024-12-01T00:00:00Z")).build()
        )).build();
        SyncResultDTO r2 = syncService.applyBatchLWW(older);
        assertThat(r2.getSkipped()).isEqualTo(1);

        StockEntity finalState = stockRepository.findById("ABC-001").orElseThrow();
        assertThat(finalState.getQuantity()).isEqualTo(15);
        assertThat(finalState.getUpdatedAt()).isEqualTo(Instant.parse("2025-02-01T00:00:00Z"));
    }
}


