package com.inventory.central.service;

import com.inventory.central.dto.StockSnapshotDTO;
import com.inventory.central.entity.StockEntity;
import com.inventory.central.exception.NotFoundException;
import com.inventory.central.repository.StockRepository;
import org.springframework.stereotype.Service;

@Service
public class StockService {
    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public StockSnapshotDTO getSnapshot(String productId) {
        StockEntity stock = stockRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Stock not found for productId=" + productId));
        return StockSnapshotDTO.builder()
                .productId(stock.getProductId())
                .quantity(stock.getQuantity())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }
}


