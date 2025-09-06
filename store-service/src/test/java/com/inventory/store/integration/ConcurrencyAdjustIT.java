package com.inventory.store.integration;

import com.inventory.store.dto.StockSnapshotDTO;
import com.inventory.store.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrencyAdjustIT {

    @Autowired
    private StockService stockService;

    @Test
    void concurrent_increments_end_with_expected_total() throws InterruptedException {
        String productId = "ABC-001";
        StockSnapshotDTO before = stockService.getSnapshot(productId);
        int tasks = 20;

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(tasks);
        for (int i = 0; i < tasks; i++) {
            executor.submit(() -> {
                try {
                    stockService.adjust(productId, 1);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        StockSnapshotDTO after = stockService.getSnapshot(productId);
        assertThat(after.getQuantity()).isEqualTo(before.getQuantity() + tasks);
    }
}


