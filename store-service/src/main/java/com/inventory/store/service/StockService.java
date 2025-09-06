package com.inventory.store.service;

import com.inventory.store.dto.StockSnapshotDTO;
import com.inventory.store.entity.StockEntity;
import com.inventory.store.exception.NotFoundException;
import com.inventory.store.repository.StockRepository;
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

