package com.inventory.store.service;

import com.inventory.store.TestClockConfig;
import com.inventory.store.dto.StockSnapshotDTO;
import com.inventory.store.exception.BadRequestException;
import com.inventory.store.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Transactional
@Import(TestClockConfig.class)
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Test
    void adjust_positive_increments_and_updates_timestamp() {
        StockSnapshotDTO before = stockService.getSnapshot("ABC-001");
        StockSnapshotDTO after = stockService.adjust("ABC-001", 2);
        assertThat(after.getQuantity()).isEqualTo(before.getQuantity() + 2);
        assertThat(after.getUpdatedAt()).isEqualTo(Instant.parse("2030-01-01T00:00:00Z"));
    }

    @Test
    void adjust_negative_valid_decrements() {
        StockSnapshotDTO before = stockService.getSnapshot("ABC-001");
        StockSnapshotDTO after = stockService.adjust("ABC-001", -1);
        assertThat(after.getQuantity()).isEqualTo(before.getQuantity() - 1);
        assertThat(after.getUpdatedAt()).isNotNull();
    }

    @Test
    void adjust_negative_below_zero_throws_bad_request() {
        assertThrows(BadRequestException.class, () -> stockService.adjust("ABC-001", -9999));
    }

    @Test
    void adjust_non_existing_product_throws_not_found() {
        assertThrows(NotFoundException.class, () -> stockService.adjust("NOPE-999", 1));
    }
}


