package com.inventory.store.init;

import com.inventory.store.entity.ProductEntity;
import com.inventory.store.entity.StockEntity;
import com.inventory.store.repository.ProductRepository;
import com.inventory.store.repository.StockRepository;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Instant;

@Component
public class DataLoader {
	private final ProductRepository productRepository;
	private final StockRepository stockRepository;

	public DataLoader(ProductRepository productRepository, StockRepository stockRepository) {
		this.productRepository = productRepository;
		this.stockRepository = stockRepository;
	}

	@PostConstruct
	public void load() {
		Instant now = Instant.now();

		ProductEntity p1 = ProductEntity.builder()
				.id("ABC-001")
				.name("Laptop Lenovo ThinkPad X1")
				.price(new BigDecimal("1500.00"))
				.updatedAt(now)
				.build();
		ProductEntity p2 = ProductEntity.builder()
				.id("ABC-002")
				.name("Smartphone Samsung Galaxy S23")
				.price(new BigDecimal("899.99"))
				.updatedAt(now)
				.build();
		ProductEntity p3 = ProductEntity.builder()
				.id("ABC-003")
				.name("Auriculares Sony WH-1000XM5")
				.price(new BigDecimal("349.99"))
				.updatedAt(now)
				.build();

		productRepository.save(p1);
		productRepository.save(p2);
		productRepository.save(p3);

		stockRepository.save(StockEntity.builder().productId("ABC-001").quantity(12).updatedAt(now).build());
		stockRepository.save(StockEntity.builder().productId("ABC-002").quantity(30).updatedAt(now).build());
		stockRepository.save(StockEntity.builder().productId("ABC-003").quantity(20).updatedAt(now).build());
	}
}

