package com.inventory.central.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncBatchDTO {
    @NotEmpty(message = "items no puede ser vacío")
    private List<StockSnapshotDTO> items;
}


