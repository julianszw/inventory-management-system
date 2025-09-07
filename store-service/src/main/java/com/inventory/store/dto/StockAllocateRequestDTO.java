package com.inventory.store.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAllocateRequestDTO {
    @NotBlank
    private String orderId;

    @NotBlank
    private String productId;

    @Min(1)
    private int quantity;
}


