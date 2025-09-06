package com.inventory.store.service;

import com.inventory.store.entity.ProductEntity;
import com.inventory.store.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public List<ProductEntity> findAll() {
		return productRepository.findAll();
	}
}

