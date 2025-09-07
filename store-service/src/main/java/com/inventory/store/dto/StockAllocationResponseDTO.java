package com.inventory.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAllocationResponseDTO {
    private String status;
    private String productId;
    private int onHand;
    private int allocated;
    private Instant updatedAt;
}


