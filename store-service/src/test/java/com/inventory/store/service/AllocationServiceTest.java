package com.inventory.store.service;

import com.inventory.store.TestClockConfig;
import com.inventory.store.dto.StockAllocateRequestDTO;
import com.inventory.store.dto.StockAllocationResponseDTO;
import com.inventory.store.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Transactional
@Import(TestClockConfig.class)
class AllocationServiceTest {

    @Autowired
    private StockService stockService;

    @Test
    void allocate_success_then_commit_reduces_onhand_and_allocated() {
        String productId = "ABC-001";
        StockAllocateRequestDTO req = StockAllocateRequestDTO.builder().orderId("o-1").productId(productId).quantity(2).build();
        StockAllocationResponseDTO alloc = stockService.allocate(null, req);
        assertThat(alloc.getStatus()).isEqualTo("ALLOCATED");
        int allocated = alloc.getAllocated();
        int onHandBeforeCommit = alloc.getOnHand();

        StockAllocationResponseDTO committed = stockService.commit(req);
        assertThat(committed.getStatus()).isEqualTo("COMMITTED");
        assertThat(committed.getAllocated()).isEqualTo(allocated - 2);
        assertThat(committed.getOnHand()).isEqualTo(onHandBeforeCommit - 2);
    }

    @Test
    void allocate_then_release_reduces_allocated_only() {
        String productId = "ABC-002";
        StockAllocateRequestDTO req = StockAllocateRequestDTO.builder().orderId("o-2").productId(productId).quantity(1).build();
        StockAllocationResponseDTO alloc = stockService.allocate(null, req);
        int allocated = alloc.getAllocated();

        StockAllocationResponseDTO released = stockService.release(req);
        assertThat(released.getStatus()).isEqualTo("RELEASED");
        assertThat(released.getAllocated()).isEqualTo(allocated - 1);
        assertThat(released.getOnHand()).isEqualTo(alloc.getOnHand());
    }

    @Test
    void allocate_insufficient_capacity_throws_409() {
        String productId = "ABC-003";
        StockAllocateRequestDTO req = StockAllocateRequestDTO.builder().orderId("o-3").productId(productId).quantity(10_000).build();
        assertThrows(BadRequestException.class, () -> stockService.allocate(null, req));
    }

    @Test
    void commit_without_reservation_throws_409() {
        String productId = "ABC-001";
        StockAllocateRequestDTO req = StockAllocateRequestDTO.builder().orderId("o-4").productId(productId).quantity(1_000).build();
        assertThrows(BadRequestException.class, () -> stockService.commit(req));
    }

    @Test
    void release_without_reservation_throws_409() {
        String productId = "ABC-001";
        StockAllocateRequestDTO req = StockAllocateRequestDTO.builder().orderId("o-5").productId(productId).quantity(1_000).build();
        assertThrows(BadRequestException.class, () -> stockService.release(req));
    }
}


