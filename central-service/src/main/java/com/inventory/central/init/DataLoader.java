package com.inventory.central.init;

import com.inventory.central.entity.ProductEntity;
import com.inventory.central.entity.StockEntity;
import com.inventory.central.repository.ProductRepository;
import com.inventory.central.repository.StockRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class DataLoader implements CommandLineRunner {
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    public DataLoader(ProductRepository productRepository, StockRepository stockRepository) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
    }

    @Override
    public void run(String... args) {
        Instant now = Instant.now();

        productRepository.save(ProductEntity.builder()
                .id("ABC-001").name("Laptop Lenovo ThinkPad X1").price(new BigDecimal("1500.00")).updatedAt(now)
                .build());
        productRepository.save(ProductEntity.builder()
                .id("ABC-002").name("Smartphone Samsung Galaxy S23").price(new BigDecimal("899.99")).updatedAt(now)
                .build());
        productRepository.save(ProductEntity.builder()
                .id("ABC-003").name("Auriculares Sony WH-1000XM5").price(new BigDecimal("349.99")).updatedAt(now)
                .build());

        stockRepository.save(StockEntity.builder().productId("ABC-001").quantity(10).updatedAt(now).build());
        stockRepository.save(StockEntity.builder().productId("ABC-002").quantity(28).updatedAt(now).build());
        stockRepository.save(StockEntity.builder().productId("ABC-003").quantity(18).updatedAt(now).build());
    }
}


