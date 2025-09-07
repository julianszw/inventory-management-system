package com.inventory.store.controller;

import com.inventory.store.dto.StockAdjustRequestDTO;
import com.inventory.store.dto.StockAllocateRequestDTO;
import com.inventory.store.dto.StockAllocationResponseDTO;
import com.inventory.store.dto.StockSnapshotDTO;
import com.inventory.store.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

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

	/**
	 * POST /stock/adjust
	 * Ejemplos:
	 * - OK: {"productId":"ABC-001","delta":5}
	 * - OK: {"productId":"ABC-001","delta":-3}
	 * - Error 400: {"productId":"ABC-001","delta":-999}
	 * - Error 404: {"productId":"NOPE-999","delta":1}
	 */
	@PostMapping("/adjust")
	public ResponseEntity<StockSnapshotDTO> adjust(@Valid @RequestBody StockAdjustRequestDTO request) {
		StockSnapshotDTO dto = stockService.adjust(request.getProductId(), request.getDelta());
		return ResponseEntity.ok(dto);
	}

	@PostMapping("/allocate")
	public ResponseEntity<StockAllocationResponseDTO> allocate(@Valid @RequestBody StockAllocateRequestDTO request,
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
		return ResponseEntity.ok(stockService.allocate(idempotencyKey, request));
	}

	@PostMapping("/commit")
	public ResponseEntity<StockAllocationResponseDTO> commit(@Valid @RequestBody StockAllocateRequestDTO request) {
		return ResponseEntity.ok(stockService.commit(request));
	}

	@PostMapping("/release")
	public ResponseEntity<StockAllocationResponseDTO> release(@Valid @RequestBody StockAllocateRequestDTO request) {
		return ResponseEntity.ok(stockService.release(request));
	}
}

