package com.inventory.central.controller;

import com.inventory.central.dto.StockSnapshotDTO;
import com.inventory.central.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock")
public class StockController {
    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockSnapshotDTO> getSnapshot(@PathVariable String productId) {
        return ResponseEntity.ok(stockService.getSnapshot(productId));
    }
}


