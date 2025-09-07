package com.inventory.store.service;

import com.inventory.store.dto.StockSnapshotDTO;
import com.inventory.store.dto.StockAllocateRequestDTO;
import com.inventory.store.dto.StockAllocationResponseDTO;
import com.inventory.store.entity.ChangeLogEntity;
import com.inventory.store.entity.IdempotencyRequestEntity;
import com.inventory.store.entity.StockEntity;
import com.inventory.store.exception.BadRequestException;
import com.inventory.store.exception.NotFoundException;
import com.inventory.store.repository.ChangeLogRepository;
import com.inventory.store.repository.IdempotencyRequestRepository;
import com.inventory.store.repository.StockRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.util.UUID;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class StockService {
	private static final Logger log = LoggerFactory.getLogger(StockService.class);

	private final StockRepository stockRepository;
	private final ChangeLogRepository changeLogRepository;
	private final IdempotencyRequestRepository idempotencyRequestRepository;
	private final Clock clock;
	private final MeterRegistry meterRegistry;
	private final Counter adjustAttempts;
	private final Counter adjustSuccess;
	private final Counter adjustFailed;
	private final Timer adjustTimer;

	public StockService(StockRepository stockRepository, ChangeLogRepository changeLogRepository, IdempotencyRequestRepository idempotencyRequestRepository, Clock clock, MeterRegistry meterRegistry) {
		this.stockRepository = stockRepository;
		this.changeLogRepository = changeLogRepository;
		this.idempotencyRequestRepository = idempotencyRequestRepository;
		this.clock = clock;
		this.meterRegistry = meterRegistry;
		this.adjustAttempts = Counter.builder("inventory_stock_adjust_attempts_total").register(meterRegistry);
		this.adjustSuccess = Counter.builder("inventory_stock_adjust_success_total").register(meterRegistry);
		this.adjustFailed = Counter.builder("inventory_stock_adjust_failed_total").register(meterRegistry);
		this.adjustTimer = Timer.builder("inventory_stock_adjust_duration_seconds").publishPercentileHistogram(true).register(meterRegistry);
	}

	public StockSnapshotDTO getSnapshot(String productId) {
		StockEntity stock = stockRepository.findById(productId)
				.orElseThrow(() -> new NotFoundException("Stock not found for productId=" + productId));
		return StockSnapshotDTO.builder()
				.productId(stock.getProductId())
				.quantity(stock.getOnHand())
				.updatedAt(stock.getUpdatedAt())
				.build();
	}

	/**
	 * Ajusta el stock de un producto aplicando un delta. Implementa bloqueo optimista con reintentos.
	 */
	public StockSnapshotDTO adjust(String productId, int delta) {
		String traceId = MDC.get("traceId");
		log.info("[traceId={}] Ajuste de stock iniciado: productId={}, delta={}", traceId, productId, delta);
		adjustAttempts.increment();
		Timer.Sample sample = Timer.start(meterRegistry);

		int maxAttempts = 3;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				StockSnapshotDTO result = doAdjust(productId, delta);
				adjustSuccess.increment();
				sample.stop(adjustTimer);
				return result;
			} catch (OptimisticLockException | ObjectOptimisticLockingFailureException ole) {
				if (attempt == maxAttempts) {
					log.error("[traceId={}] Error de concurrencia tras {} intentos", traceId, attempt);
					adjustFailed.increment();
					sample.stop(adjustTimer);
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

		int newQty = stock.getOnHand() + delta;
		if (newQty < 0) {
			log.warn("[traceId={}] Ajuste inválido: resultaría negativo. productId={}, delta={}, existente={}", traceId, productId, delta, stock.getOnHand());
			throw new BadRequestException("El stock resultante no puede ser negativo");
		}

		Instant now = clock.instant();
		stock.setOnHand(newQty);
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

	public StockAllocationResponseDTO allocate(String idempotencyKey, StockAllocateRequestDTO request) {
		if (request.getQuantity() <= 0) {
			throw new BadRequestException("quantity debe ser > 0");
		}
		if (idempotencyKey != null && !idempotencyKey.isBlank()) {
			if (idempotencyRequestRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
				StockEntity st = stockRepository.findById(request.getProductId())
						.orElseThrow(() -> new NotFoundException("Stock not found for productId=" + request.getProductId()));
				return StockAllocationResponseDTO.builder()
						.status("ALLOCATED")
						.productId(st.getProductId())
						.onHand(st.getOnHand())
						.allocated(st.getAllocated())
						.updatedAt(st.getUpdatedAt())
						.build();
			}
		}

		int maxAttempts = 3;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return doAllocate(idempotencyKey, request);
			} catch (OptimisticLockException | ObjectOptimisticLockingFailureException ole) {
				if (attempt == maxAttempts) {
					throw ole;
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
	protected StockAllocationResponseDTO doAllocate(String idempotencyKey, StockAllocateRequestDTO request) {
		Instant now = clock.instant();
		StockEntity stock = stockRepository.findById(request.getProductId())
				.orElseThrow(() -> new NotFoundException("Stock not found for productId=" + request.getProductId()));
		int available = stock.getOnHand() - stock.getAllocated();
		if (available < request.getQuantity()) {
			throw new BadRequestException("No hay stock disponible para reservar");
		}
		stock.setAllocated(stock.getAllocated() + request.getQuantity());
		stock.setUpdatedAt(now);
		stockRepository.saveAndFlush(stock);

		changeLogRepository.save(ChangeLogEntity.builder()
				.id(UUID.randomUUID())
				.productId(stock.getProductId())
				.updatedAt(now)
				.build());

		if (idempotencyKey != null && !idempotencyKey.isBlank()) {
			idempotencyRequestRepository.save(IdempotencyRequestEntity.builder()
					.id(UUID.randomUUID())
					.idempotencyKey(idempotencyKey)
					.requestHash(request.getOrderId() + ":" + request.getProductId() + ":" + request.getQuantity())
					.createdAt(now)
					.build());
		}
		return StockAllocationResponseDTO.builder()
				.status("ALLOCATED")
				.productId(stock.getProductId())
				.onHand(stock.getOnHand())
				.allocated(stock.getAllocated())
				.updatedAt(now)
				.build();
	}

	public StockAllocationResponseDTO commit(StockAllocateRequestDTO request) {
		if (request.getQuantity() <= 0) {
			throw new BadRequestException("quantity debe ser > 0");
		}
		int maxAttempts = 3;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return doCommit(request);
			} catch (OptimisticLockException | ObjectOptimisticLockingFailureException ole) {
				if (attempt == maxAttempts) {
					throw ole;
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
	protected StockAllocationResponseDTO doCommit(StockAllocateRequestDTO request) {
		Instant now = clock.instant();
		StockEntity stock = stockRepository.findById(request.getProductId())
				.orElseThrow(() -> new NotFoundException("Stock not found for productId=" + request.getProductId()));
		if (stock.getAllocated() < request.getQuantity()) {
			throw new BadRequestException("Reserva insuficiente para commit");
		}
		stock.setOnHand(stock.getOnHand() - request.getQuantity());
		stock.setAllocated(stock.getAllocated() - request.getQuantity());
		stock.setUpdatedAt(now);
		stockRepository.saveAndFlush(stock);
		changeLogRepository.save(ChangeLogEntity.builder()
				.id(UUID.randomUUID())
				.productId(stock.getProductId())
				.updatedAt(now)
				.build());
		return StockAllocationResponseDTO.builder()
				.status("COMMITTED")
				.productId(stock.getProductId())
				.onHand(stock.getOnHand())
				.allocated(stock.getAllocated())
				.updatedAt(now)
				.build();
	}

	public StockAllocationResponseDTO release(StockAllocateRequestDTO request) {
		if (request.getQuantity() <= 0) {
			throw new BadRequestException("quantity debe ser > 0");
		}
		int maxAttempts = 3;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				return doRelease(request);
			} catch (OptimisticLockException | ObjectOptimisticLockingFailureException ole) {
				if (attempt == maxAttempts) {
					throw ole;
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
	protected StockAllocationResponseDTO doRelease(StockAllocateRequestDTO request) {
		Instant now = clock.instant();
		StockEntity stock = stockRepository.findById(request.getProductId())
				.orElseThrow(() -> new NotFoundException("Stock not found for productId=" + request.getProductId()));
		if (stock.getAllocated() < request.getQuantity()) {
			throw new BadRequestException("Reserva insuficiente para release");
		}
		stock.setAllocated(stock.getAllocated() - request.getQuantity());
		stock.setUpdatedAt(now);
		stockRepository.saveAndFlush(stock);
		changeLogRepository.save(ChangeLogEntity.builder()
				.id(UUID.randomUUID())
				.productId(stock.getProductId())
				.updatedAt(now)
				.build());
		return StockAllocationResponseDTO.builder()
				.status("RELEASED")
				.productId(stock.getProductId())
				.onHand(stock.getOnHand())
				.allocated(stock.getAllocated())
				.updatedAt(now)
				.build();
	}
}

