package com.inventory.store.repository;

import com.inventory.store.entity.IdempotencyRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRequestRepository extends JpaRepository<IdempotencyRequestEntity, UUID> {
    Optional<IdempotencyRequestEntity> findByIdempotencyKey(String idempotencyKey);
}


