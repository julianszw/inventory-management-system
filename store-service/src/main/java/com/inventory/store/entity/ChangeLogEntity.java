package com.inventory.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox mínimo para sincronización tienda→central
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "change_log", indexes = {
		@Index(name = "idx_change_log_updated_at", columnList = "updated_at")
})
public class ChangeLogEntity {
	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "product_id", nullable = false, length = 64)
	private String productId;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}


