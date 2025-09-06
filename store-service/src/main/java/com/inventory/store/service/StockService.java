package com.inventory.store.service;

import com.inventory.store.dto.StockSnapshotDTO;
import com.inventory.store.entity.ChangeLogEntity;
import com.inventory.store.entity.StockEntity;
import com.inventory.store.exception.BadRequestException;
import com.inventory.store.exception.NotFoundException;
import com.inventory.store.repository.ChangeLogRepository;
import com.inventory.store.repository.StockRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class StockService {
	private static final Logger log = LoggerFactory.getLogger(StockService.class);

	private final StockRepository stockRepository;
	private final ChangeLogRepository changeLogRepository;

	public StockService(StockRepository stockRepository, ChangeLogRepository changeLogRepository) {
		this.stockRepository = stockRepository;
		this.changeLogRepository = changeLogRepository;
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

	/**
	 * Ajusta el stock de un producto aplicando un delta. Implementa bloqueo optimista con reintentos.
	 */
	public StockSnapshotDTO adjust(String productId, int delta) {
		String traceId = MDC.get("traceId");
		log.info("[traceId={}] Ajuste de stock iniciado: productId={}, delta={}", traceId, productId, delta);

		int maxAttempts = 3;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return doAdjust(productId, delta);
			} catch (OptimisticLockException | ObjectOptimisticLockingFailureException ole) {
				if (attempt == maxAttempts) {
					log.error("[traceId={}] Error de concurrencia tras {} intentos", traceId, attempt);
					throw new RuntimeException("No se pudo completar el ajuste por concurrencia. Intente nuevamente.", ole);
				}
				try {
					Thread.sleep(50L * attempt);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Operación interrumpida durante reintento", ie);
				}
			}
		}
		throw new IllegalStateException("Unreachable");
	}

	@Transactional
	protected StockSnapshotDTO doAdjust(String productId, int delta) {
		String traceId = MDC.get("traceId");

		StockEntity stock = stockRepository.findById(productId)
				.orElseThrow(() -> new NotFoundException("Stock not found for productId=" + productId));

		int newQty = stock.getQuantity() + delta;
		if (newQty < 0) {
			log.warn("[traceId={}] Ajuste inválido: resultaría negativo. productId={}, delta={}, existente={}", traceId, productId, delta, stock.getQuantity());
			throw new BadRequestException("El stock resultante no puede ser negativo");
		}

		Instant now = Instant.now();
		stock.setQuantity(newQty);
		stock.setUpdatedAt(now);
		stockRepository.saveAndFlush(stock);

		ChangeLogEntity change = ChangeLogEntity.builder()
				.id(UUID.randomUUID())
				.productId(productId)
				.updatedAt(now)
				.build();
		changeLogRepository.save(change);

		log.info("[traceId={}] Ajuste de stock exitoso: productId={}, newQty={}, updatedAt={}", traceId, productId, newQty, now);

		return StockSnapshotDTO.builder()
				.productId(productId)
				.quantity(newQty)
				.updatedAt(now)
				.build();
	}
}

