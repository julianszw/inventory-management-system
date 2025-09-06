package com.inventory.store.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustRequestDTO {
    @NotBlank
    private String productId;

    private int delta;

    @AssertTrue(message = "delta debe ser distinto de 0")
    public boolean isDeltaNonZero() {
        return delta != 0;
    }
}


