package com.inventory.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSnapshotDTO {
    private String productId;
    private int quantity;
    private Instant updatedAt;
}


