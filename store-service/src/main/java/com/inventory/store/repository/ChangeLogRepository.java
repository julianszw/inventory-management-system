package com.inventory.store.repository;

import com.inventory.store.entity.ChangeLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChangeLogRepository extends JpaRepository<ChangeLogEntity, UUID> {
}


